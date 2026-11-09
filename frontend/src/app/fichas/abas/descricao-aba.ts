import { ChangeDetectorRef, Component, ElementRef, Injector, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { DescricaoFicha, Ficha } from '../ficha.model';
import { aplicarErrosDoServidor, focarPrimeiroInvalido, focarPrimeiroInvalidoDepois, mensagemDoControle } from '../sistemas/comum/formulario-comum';

export const DESCRICAO_MAXIMA = 4000;

type Valores = { aparencia: string; historia: string; anotacoes: string };

function valoresDe(d: DescricaoFicha): Valores {
  return { aparencia: d.aparencia ?? '', historia: d.historia ?? '', anotacoes: d.anotacoes ?? '' };
}

/**
 * Aba Descrição: três textos longos (aparência, história e anotações). Nada é salvo sozinho: só o
 * botão grava. O botão fica desligado enquanto não houver mudança desde o último salvamento.
 */
@Component({
  selector: 'app-descricao-aba',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule],
  templateUrl: './descricao-aba.html',
  styleUrl: './descricao-aba.scss',
})
export class DescricaoAba implements OnInit {
  private readonly api = inject(FichaApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly ficha = input.required<Ficha>();
  /** A descrição depois de gravada; o detalhe a coloca na ficha. */
  readonly atualizada = output<DescricaoFicha>();

  protected readonly salvando = signal(false);
  protected readonly salvo = signal(false);
  protected readonly erroGeral = signal<ApiError | null>(null);
  protected readonly maximo = DESCRICAO_MAXIMA;

  protected readonly campos = [
    { nome: 'aparencia', rotulo: 'Aparência' },
    { nome: 'historia', rotulo: 'História' },
    { nome: 'anotacoes', rotulo: 'Anotações' },
  ] as const;

  protected readonly form = inject(NonNullableFormBuilder).group({
    aparencia: ['', [Validators.maxLength(DESCRICAO_MAXIMA)]],
    historia: ['', [Validators.maxLength(DESCRICAO_MAXIMA)]],
    anotacoes: ['', [Validators.maxLength(DESCRICAO_MAXIMA)]],
  });

  /** O que está gravado (a base de comparação do "houve mudança?"). */
  private readonly base = signal<Valores>({ aparencia: '', historia: '', anotacoes: '' });
  private readonly atuais = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly semMudanca = computed(() => {
    const base = this.base();
    const atuais = this.atuais();
    return this.campos.every(({ nome }) => (atuais[nome] ?? '') === base[nome]);
  });

  ngOnInit(): void {
    this.carregar(this.ficha().descricao);
  }

  protected tamanho(nome: keyof Valores): number {
    return (this.atuais()[nome] ?? '').length;
  }

  protected mensagem = mensagemDoControle;

  protected salvar(): void {
    if (this.salvando() || this.semMudanca()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      focarPrimeiroInvalido(this.host.nativeElement);
      return;
    }
    const v = this.form.getRawValue();
    const entrada: DescricaoFicha = {
      aparencia: v.aparencia.trim() === '' ? null : v.aparencia,
      historia: v.historia.trim() === '' ? null : v.historia,
      anotacoes: v.anotacoes.trim() === '' ? null : v.anotacoes,
    };
    this.erroGeral.set(null);
    this.salvo.set(false);
    this.salvando.set(true);
    // Bloqueia a digitação: o que fosse digitado agora seria apagado pelo reset do sucesso.
    this.form.disable({ emitEvent: false });
    this.api.atualizarDescricao(this.ficha().id, entrada).subscribe({
      next: (gravada) => {
        this.salvando.set(false);
        this.form.enable({ emitEvent: false });
        this.carregar(gravada);
        this.salvo.set(true);
        this.atualizada.emit(gravada);
      },
      error: (erro: ApiError) => {
        this.salvando.set(false);
        this.form.enable({ emitEvent: false });
        if (aplicarErrosDoServidor(this.form, erro)) {
          focarPrimeiroInvalidoDepois(this.host.nativeElement, this.injector, this.cdr);
        } else {
          this.erroGeral.set(erro);
        }
      },
    });
  }

  /** Põe a descrição nos campos e a marca como a gravada. */
  private carregar(descricao: DescricaoFicha): void {
    const valores = valoresDe(descricao);
    this.base.set(valores);
    this.form.reset(valores);
  }
}
