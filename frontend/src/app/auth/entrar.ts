import { Component, ElementRef, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../core/api-error';
import { AuthApi } from '../core/auth/auth-api';
import { voltaSegura } from '../core/auth/guards';
import { Sessao } from '../core/auth/sessao';
import { aplicarErrosDoServidor, focarPrimeiroInvalido } from '../fichas/sistemas/comum/formulario-comum';
import { BotaoGoogle } from './botao-google';
import { mensagemAuth } from './mensagem-auth';

/** Tela de entrar. O `volta` (query param, ligado ao input pelo roteador) diz para onde ir depois. */
@Component({
  selector: 'app-entrar',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, RouterLink, BotaoGoogle],
  templateUrl: './entrar.html',
  styleUrl: './entrar.scss',
})
export class Entrar {
  private readonly api = inject(AuthApi);
  private readonly sessao = inject(Sessao);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly volta = input<string | undefined>();

  protected readonly enviando = signal(false);
  protected readonly mostrarSenha = signal(false);
  protected readonly erroGeral = signal<string | null>(null);
  protected readonly mensagem = mensagemAuth;
  protected readonly capsLigado = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    senha: ['', [Validators.required]],
  });

  /** Caps Lock ligado? A pessoa não vê a senha, então o aviso evita um "senha incorreta" à toa. */
  protected atualizarCapsLock(evento: KeyboardEvent): void {
    this.capsLigado.set(evento.getModifierState('CapsLock'));
  }

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
    this.api.entrar(this.form.getRawValue()).subscribe({
      next: (resposta) => {
        this.sessao.iniciar(resposta);
        void this.router.navigateByUrl(voltaSegura(this.volta()));
      },
      error: (erro: ApiError) => {
        this.enviando.set(false);
        this.form.enable({ emitEvent: false });
        if (!aplicarErrosDoServidor(this.form, erro)) {
          // O 401 vem como "E-mail ou senha incorretos", igual para os dois casos (a API não revela qual).
          this.erroGeral.set(erro.detalhe || erro.titulo);
        }
      },
    });
  }
}
