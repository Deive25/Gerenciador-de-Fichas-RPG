import { Component, ElementRef, effect, inject, input, output, signal, untracked } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiError } from '../core/api-error';
import {
  aplicarErrosDoServidor,
  focarPrimeiroInvalido,
  mensagemDoControle,
  textoOuNulo,
} from '../fichas/sistemas/comum/formulario-comum';
import { CampanhaEntrada } from './campanha.model';

/** Texto só com espaços não conta como preenchido (a API também tira os espaços das pontas). */
function naoBranco(controle: AbstractControl): ValidationErrors | null {
  return typeof controle.value === 'string' && controle.value.trim() === '' ? { required: true } : null;
}

/** Nome e descrição de uma campanha: serve à criação (Minhas campanhas) e à edição (Campanha). */
@Component({
  selector: 'app-formulario-campanha',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule],
  templateUrl: './formulario-campanha.html',
  styleUrl: './formulario-campanha.scss',
})
export class FormularioCampanha {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Valores de partida (edição); sem ele o formulário começa vazio. */
  readonly inicial = input<CampanhaEntrada>();
  readonly rotulo = input('Salvar');
  readonly enviando = input(false);
  /** O erro da última tentativa de salvar: o de campo vai para o campo, os outros para um alerta. */
  readonly erro = input<ApiError | null>(null);

  readonly enviar = output<CampanhaEntrada>();
  readonly cancelar = output<void>();

  protected readonly erroGeral = signal<string | null>(null);
  protected readonly mensagem = mensagemDoControle;

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, naoBranco, Validators.maxLength(100)]],
    descricao: ['', [Validators.maxLength(1000)]],
  });

  constructor() {
    effect(() => {
      const inicial = this.inicial();
      if (inicial) {
        untracked(() => this.form.setValue({ nome: inicial.nome, descricao: inicial.descricao ?? '' }));
      }
    });
    effect(() => {
      const erro = this.erro();
      untracked(() => {
        this.erroGeral.set(null);
        if (erro && !aplicarErrosDoServidor(this.form, erro)) {
          this.erroGeral.set(erro.detalhe || erro.titulo);
        }
      });
    });
  }

  protected enviarForm(): void {
    if (this.enviando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      focarPrimeiroInvalido(this.host.nativeElement);
      return;
    }
    const { nome, descricao } = this.form.getRawValue();
    this.enviar.emit({ nome: nome.trim(), descricao: textoOuNulo(descricao) });
  }
}
