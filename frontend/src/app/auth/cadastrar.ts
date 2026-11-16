import { ChangeDetectorRef, Component, ElementRef, Injector, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../core/api-error';
import { AuthApi } from '../core/auth/auth-api';
import { voltaSegura } from '../core/auth/guards';
import { Sessao } from '../core/auth/sessao';
import { aplicarErrosDoServidor, focarPrimeiroInvalido, focarPrimeiroInvalidoDepois } from '../fichas/sistemas/comum/formulario-comum';
import { mensagemAuth } from './mensagem-auth';

/** Tela de criar conta. O cadastro já entra na conta (a API devolve o par de tokens). */
@Component({
  selector: 'app-cadastrar',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, RouterLink],
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

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    senha: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  protected enviar(): void {
    if (this.enviando()) {
      return;
    }
    if (this.form.invalid) {
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
