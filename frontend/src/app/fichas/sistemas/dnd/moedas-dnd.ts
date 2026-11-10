import { ChangeDetectorRef, Component, ElementRef, Injector, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { FichaDnd, Moedas } from '../../ficha.model';
import {
  INTEIRO,
  aplicarErrosDoServidor,
  faixa,
  focarPrimeiroInvalido,
  focarPrimeiroInvalidoDepois,
  mensagemDoControle,
  textoDe,
} from '../comum/formulario-comum';

type Moeda = keyof Moedas;

/** As cinco moedas, com a sigla no rótulo e o nome por extenso para o leitor de tela. */
const CAMPOS: readonly { nome: Moeda; rotulo: string; nomeCompleto: string }[] = [
  { nome: 'pc', rotulo: 'PC', nomeCompleto: 'peças de cobre' },
  { nome: 'pp', rotulo: 'PP', nomeCompleto: 'peças de prata' },
  { nome: 'pe', rotulo: 'PE', nomeCompleto: 'peças de electro' },
  { nome: 'po', rotulo: 'PO', nomeCompleto: 'peças de ouro' },
  { nome: 'pl', rotulo: 'PL', nomeCompleto: 'peças de platina' },
];

/** O maior inteiro que o servidor aceita (int de 32 bits). */
const INTEIRO_MAXIMO = 2147483647;
const VALIDADORES = [Validators.required, Validators.pattern(INTEIRO), faixa(0, INTEIRO_MAXIMO)];

type Valores = Record<Moeda, string>;

function valoresDe(m: Moedas): Valores {
  return { pc: textoDe(m.pc), pp: textoDe(m.pp), pe: textoDe(m.pe), po: textoDe(m.po), pl: textoDe(m.pl) };
}

/**
 * Moedas da ficha de D&D: cinco inteiros ≥ 0 que se gravam juntos, pelo botão (nada salva sozinho).
 * Mesmo desenho da DescricaoAba: o botão só habilita com mudança, os campos ficam bloqueados enquanto grava
 * e o aviso "Moedas salvas." fica sempre na página (role="status"), vazio até salvar.
 */
@Component({
  selector: 'app-moedas-dnd',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule],
  templateUrl: './moedas-dnd.html',
  styleUrl: './moedas-dnd.scss',
})
export class MoedasDnd implements OnInit {
  private readonly api = inject(FichaApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly ficha = input.required<FichaDnd>();
  /** As cinco moedas depois de gravadas; o detalhe as coloca na ficha. */
  readonly atualizadas = output<Moedas>();

  protected readonly campos = CAMPOS;
  protected readonly salvando = signal(false);
  protected readonly salvo = signal(false);
  protected readonly erroGeral = signal<ApiError | null>(null);
  protected readonly mensagem = mensagemDoControle;

  protected readonly form = inject(NonNullableFormBuilder).group({
    pc: ['0', VALIDADORES],
    pp: ['0', VALIDADORES],
    pe: ['0', VALIDADORES],
    po: ['0', VALIDADORES],
    pl: ['0', VALIDADORES],
  });

  /** O que está gravado (a base do "houve mudança?"). */
  private readonly base = signal<Valores>({ pc: '0', pp: '0', pe: '0', po: '0', pl: '0' });
  private readonly atuais = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  private readonly semMudanca = computed(() => {
    const base = this.base();
    const atuais = this.atuais();
    return CAMPOS.every(({ nome }) => (atuais[nome] ?? '') === base[nome]);
  });
  /** O botão também espera o formulário ser válido. */
  protected readonly botaoBloqueado = computed(() => {
    this.atuais();
    return this.semMudanca() || this.form.invalid;
  });
  protected readonly confirmado = computed(() => this.salvo() && this.semMudanca());

  ngOnInit(): void {
    this.carregar(this.ficha().moedas);
  }

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
    const entrada: Moedas = { pc: Number(v.pc), pp: Number(v.pp), pe: Number(v.pe), po: Number(v.po), pl: Number(v.pl) };
    this.erroGeral.set(null);
    this.salvo.set(false);
    this.salvando.set(true);
    // Bloqueia a digitação: o que fosse digitado agora seria apagado pelo reset do sucesso.
    this.form.disable({ emitEvent: false });
    this.api.atualizarMoedas(this.ficha().id, entrada).subscribe({
      next: (gravadas) => {
        this.salvando.set(false);
        this.form.enable({ emitEvent: false });
        this.carregar(gravadas);
        this.salvo.set(true);
        this.atualizadas.emit(gravadas);
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

  /** Põe as moedas nos campos e as marca como as gravadas. */
  private carregar(moedas: Moedas): void {
    const valores = valoresDe(moedas);
    this.base.set(valores);
    this.form.reset(valores);
  }
}
