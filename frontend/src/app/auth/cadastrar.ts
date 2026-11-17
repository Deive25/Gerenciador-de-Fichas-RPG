import { ChangeDetectorRef, Component, ElementRef, Injector, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { merge } from 'rxjs';
import { ApiError } from '../core/api-error';
import { AuthApi } from '../core/auth/auth-api';
import { voltaSegura } from '../core/auth/guards';
import { Sessao } from '../core/auth/sessao';
import { aplicarErrosDoServidor, focarPrimeiroInvalido, focarPrimeiroInvalidoDepois } from '../fichas/sistemas/comum/formulario-comum';
import { BotaoGoogle } from './botao-google';
import { mensagemAuth } from './mensagem-auth';
import { requisitosDaSenha, tamanhoDaSenha } from './politica-de-senha';

/** Inválida enquanto algum requisito da lista falhar. Lê o nome e o e-mail do grupo (a regra "sem nome nem e-mail"). */
const requisitosAtendidos: ValidatorFn = (controle) => {
  const grupo = controle.parent;
  const nome = (grupo?.get('nome')?.value as string | undefined) ?? '';
  const email = (grupo?.get('email')?.value as string | undefined) ?? '';
  const todos = requisitosDaSenha((controle.value as string) ?? '', nome, email).every((r) => r.ok);
  return todos ? null : { requisitos: true };
};

/** Tela de criar conta. O cadastro já entra na conta (a API devolve o par de tokens). */
@Component({
  selector: 'app-cadastrar',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, RouterLink, BotaoGoogle],
  templateUrl: './cadastrar.html',
  styleUrl: './cadastrar.scss',
})
export class Cadastrar {
  private readonly api = inject(AuthApi);
  private readonly sessao = inject(Sessao);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly volta = input<string | undefined>();

  protected readonly enviando = signal(false);
  protected readonly mostrarSenha = signal(false);
  protected readonly erroGeral = signal<string | null>(null);
  protected readonly mensagem = mensagemAuth;
  protected readonly capsLigado = signal(false);
  /** A lista de requisitos só aparece depois que a pessoa começa a digitar a senha ou tenta enviar. */
  protected readonly listaVisivel = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    senha: ['', [Validators.required, Validators.maxLength(72), requisitosAtendidos]],
  });

  private readonly nome = toSignal(this.form.controls.nome.valueChanges, { initialValue: '' });
  private readonly email = toSignal(this.form.controls.email.valueChanges, { initialValue: '' });
  private readonly senha = toSignal(this.form.controls.senha.valueChanges, { initialValue: '' });

  protected readonly requisitos = computed(() => requisitosDaSenha(this.senha(), this.nome(), this.email()));
  protected readonly tamanho = computed(() => tamanhoDaSenha(this.senha()));

  constructor() {
    // O nome e o e-mail fazem parte das regras da senha: ao mudar, a senha é validada de novo.
    merge(this.form.controls.nome.valueChanges, this.form.controls.email.valueChanges)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.form.controls.senha.updateValueAndValidity({ emitEvent: false }));
    this.form.controls.senha.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.listaVisivel.set(true));
  }

  /** Caps Lock ligado? A pessoa não vê a senha, então o aviso evita erro de digitação à toa. */
  protected atualizarCapsLock(evento: KeyboardEvent): void {
    this.capsLigado.set(evento.getModifierState('CapsLock'));
  }

  protected enviar(): void {
    if (this.enviando()) {
      return;
    }
    if (this.form.invalid) {
      this.listaVisivel.set(true);
      this.form.markAllAsTouched();
      focarPrimeiroInvalido(this.host.nativeElement);
      return;
    }
    this.erroGeral.set(null);
    this.enviando.set(true);
    this.form.disable({ emitEvent: false });
    const { nome, email, senha } = this.form.getRawValue();
    this.api.registrar({ nome: nome.trim(), email: email.trim(), senha }).subscribe({
      next: (resposta) => {
        this.sessao.iniciar(resposta);
        void this.router.navigateByUrl(voltaSegura(this.volta()));
      },
      error: (erro: ApiError) => {
        this.enviando.set(false);
        this.form.enable({ emitEvent: false });
        if (erro.status === 409 && !erro.campos['email']) {
          // O 409 é sempre "e-mail já cadastrado": vai para o campo mesmo que a API não traga `errors`.
          erro = { ...erro, campos: { email: erro.detalhe || erro.titulo } };
        }
        if (aplicarErrosDoServidor(this.form, erro)) {
          focarPrimeiroInvalidoDepois(this.host.nativeElement, this.injector, this.cdr);
        } else {
          this.erroGeral.set(erro.detalhe || erro.titulo);
        }
      },
    });
  }
}
