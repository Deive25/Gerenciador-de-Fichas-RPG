import { ChangeDetectorRef, Component, ElementRef, Injector, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import { Espacos, FichaDnd, NivelEspaco } from '../../ficha.model';
import {
  INTEIRO,
  aplicarErrosDoServidor,
  faixa,
  focarPrimeiroInvalido,
  focarPrimeiroInvalidoDepois,
  mensagemDoControle,
  textoDe,
} from '../comum/formulario-comum';
import { rotuloNivelMagia } from './regras-dnd';

const NIVEIS: readonly NivelEspaco[] = ['1', '2', '3', '4', '5', '6', '7', '8', '9'];

/**
 * Teto só da tela: a API aceita qualquer inteiro ≥ 0, mas cada espaço vira um botão, e um total
 * absurdo travaria a página. No livro o máximo por nível é 4.
 */
const TOTAL_MAXIMO = 99;
const VALIDADORES = [Validators.required, Validators.pattern(INTEIRO), faixa(0, TOTAL_MAXIMO)];

type Valores = Record<NivelEspaco, string>;

function totaisDe(espacos: Espacos): Valores {
  return Object.fromEntries(NIVEIS.map((n) => [n, textoDe(espacos[n].total)])) as Valores;
}

/**
 * Espaços de magia da ficha de D&D (decisão 6 do plano do 2.1j). Cada nível com total > 0 mostra um
 * marcador por espaço: clicar em um vazio soma 1 em `usados`, clicar em um marcado subtrai 1, e cada
 * clique já grava (PATCH só daquele nível). Os totais se editam num formulário com botão (nada salva
 * sozinho), e "Descanso longo" zera os usados de todos os níveis. O dado vem sempre de `ficha().espacos`:
 * a aba grava, avisa os 9 níveis (`atualizados`) e o detalhe os coloca na ficha.
 */
@Component({
  selector: 'app-espacos-dnd',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule],
  templateUrl: './espacos-dnd.html',
  styleUrl: './espacos-dnd.scss',
})
export class EspacosDnd implements OnInit {
  private readonly api = inject(FichaApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly ficha = input.required<FichaDnd>();
  /** Os 9 níveis depois de gravados; o detalhe os coloca na ficha. */
  readonly atualizados = output<Espacos>();

  protected readonly niveis = NIVEIS;
  protected readonly salvando = signal(false);
  /** O rótulo "Salvando…" é só do botão de totais; nos cliques dos marcadores ele não muda. */
  protected readonly salvandoTotais = signal(false);
  /** O que acabou de acontecer, para o leitor de tela (vazio até a primeira gravação). */
  protected readonly aviso = signal('');
  protected readonly erroGeral = signal<ApiError | null>(null);
  protected readonly mensagem = mensagemDoControle;
  protected readonly rotuloNivel = rotuloNivelMagia;

  protected readonly form = inject(NonNullableFormBuilder).group({
    '1': ['0', VALIDADORES],
    '2': ['0', VALIDADORES],
    '3': ['0', VALIDADORES],
    '4': ['0', VALIDADORES],
    '5': ['0', VALIDADORES],
    '6': ['0', VALIDADORES],
    '7': ['0', VALIDADORES],
    '8': ['0', VALIDADORES],
    '9': ['0', VALIDADORES],
  });

  /** Os totais gravados (a base do "houve mudança?"). */
  private readonly base = signal<Valores>(this.form.getRawValue());
  private readonly atuais = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  private readonly semMudanca = computed(() => {
    const base = this.base();
    const atuais = this.atuais();
    return NIVEIS.every((n) => (atuais[n] ?? '') === base[n]);
  });
  protected readonly botaoBloqueado = computed(() => {
    this.atuais();
    return this.semMudanca() || this.form.invalid;
  });

  /** Só os níveis com espaço: um marcador por espaço, os primeiros `usados` marcados. */
  protected readonly comEspacos = computed(() =>
    NIVEIS.map((nivel) => ({ nivel, ...this.ficha().espacos[nivel] }))
      .filter((e) => e.total > 0)
      .map((e) => ({
        ...e,
        marcadores: Array.from({ length: e.total }, (_, i) => ({
          indice: i,
          usado: i < e.usados,
          rotulo: `${e.nivel}º nível, espaço ${i + 1} de ${e.total}, ${i < e.usados ? 'usado' : 'disponível'}`,
        })),
      })),
  );
  protected readonly algumUsado = computed(() => NIVEIS.some((n) => this.ficha().espacos[n].usados > 0));

  ngOnInit(): void {
    this.carregar(this.ficha().espacos);
  }

  /** Clicar em um espaço vazio usa um; clicar em um marcado devolve um. Qual deles é clicado não importa. */
  protected alternar(nivel: NivelEspaco, indice: number): void {
    const { total, usados } = this.ficha().espacos[nivel];
    const novo = indice < usados ? usados - 1 : usados + 1;
    this.enviar({ [nivel]: { total, usados: novo } }, (r) => `${nivel}º nível: ${r[nivel].usados} de ${r[nivel].total} espaços usados.`);
  }

  protected descansar(): void {
    const parcial: Partial<Espacos> = {};
    for (const n of NIVEIS) {
      const { total, usados } = this.ficha().espacos[n];
      if (usados > 0) {
        parcial[n] = { total, usados: 0 };
      }
    }
    if (Object.keys(parcial).length > 0) {
      this.enviar(parcial, () => 'Descanso longo: todos os espaços recuperados.');
    }
  }

  protected salvarTotais(): void {
    if (this.salvando() || this.semMudanca()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      focarPrimeiroInvalido(this.host.nativeElement);
      return;
    }
    const valores = this.form.getRawValue();
    const base = this.base();
    const parcial: Partial<Espacos> = {};
    for (const n of NIVEIS) {
      if (valores[n] !== base[n]) {
        const total = Number(valores[n]);
        // Total abaixo dos usados: o servidor recusa usados > total, então os usados descem junto.
        parcial[n] = { total, usados: Math.min(this.ficha().espacos[n].usados, total) };
      }
    }
    // Bloqueia a digitação: o que fosse digitado agora seria apagado pelo reset do sucesso.
    this.form.disable({ emitEvent: false });
    this.salvandoTotais.set(true);
    this.enviar(
      parcial,
      () => 'Totais salvos.',
      (gravados) => {
        this.salvandoTotais.set(false);
        this.form.enable({ emitEvent: false });
        this.carregar(gravados);
      },
      (erro) => {
        this.salvandoTotais.set(false);
        this.form.enable({ emitEvent: false });
        // "3.total" e "3.usados" apontam para o campo do nível 3 (o campo é só o total).
        const campos = Object.fromEntries(Object.entries(erro.campos).map(([chave, texto]) => [chave.split('.')[0], texto]));
        if (aplicarErrosDoServidor(this.form, { ...erro, campos })) {
          focarPrimeiroInvalidoDepois(this.host.nativeElement, this.injector, this.cdr);
        } else {
          this.erroGeral.set(erro);
        }
      },
    );
  }

  private enviar(
    parcial: Partial<Espacos>,
    avisoDeSucesso: (gravados: Espacos) => string,
    aoGravar: (gravados: Espacos) => void = () => undefined,
    aoFalhar: (erro: ApiError) => void = (erro) => this.erroGeral.set(erro),
  ): void {
    if (this.salvando()) {
      return;
    }
    this.erroGeral.set(null);
    this.aviso.set('');
    this.salvando.set(true);
    this.api.atualizarEspacos(this.ficha().id, parcial).subscribe({
      next: (gravados) => {
        this.salvando.set(false);
        aoGravar(gravados);
        this.aviso.set(avisoDeSucesso(gravados));
        this.atualizados.emit(gravados);
      },
      error: (erro: ApiError) => {
        this.salvando.set(false);
        aoFalhar(erro);
      },
    });
  }

  /** Põe os totais nos campos e os marca como os gravados. */
  private carregar(espacos: Espacos): void {
    const valores = totaisDe(espacos);
    this.base.set(valores);
    this.form.reset(valores);
  }
}
