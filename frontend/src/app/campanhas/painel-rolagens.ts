import { Component, DestroyRef, ElementRef, afterNextRender, computed, effect, inject, input, output, signal, untracked, viewChild } from '@angular/core';
import { Subscription, catchError, of } from 'rxjs';
import { ApiError } from '../core/api-error';
import { CampanhaApi } from './campanha-api';
import { MesaAoVivo } from './mesa-ao-vivo';
import {
  RolagemDaMesa,
  TAMANHO_DA_PAGINA,
  horaRelativa,
  juntarRolagens,
  quemRolou,
} from './rolagem-da-mesa.model';
import { RolagensApi } from './rolagens-api';

/** Um dado na linha pequena: o valor e se foi o mantido (em negrito). */
interface DadoDaLinha {
  valor: number;
  mantido: boolean;
}

/** O que a linha menor de cada rolagem mostra, já pronto para o template (sem HTML dinâmico). */
interface Detalhes {
  dados: DadoDaLinha[];
  /** "20 natural" / "1 natural" quando o dado mantido do d20 foi 20 ou 1. */
  natural: string | null;
  critico: boolean;
  /** Só no ataque: "dano 14 (2d8+3)". */
  dano: string | null;
  /** Soma fixa depois dos dados ("+5", "-2"), ou null quando é zero. */
  bonus: string | null;
}

const doBonus = (bonus: number): string | null => (bonus === 0 ? null : bonus > 0 ? `+${bonus}` : `${bonus}`);

const naturalDe = (mantido: number): string | null => (mantido === 20 || mantido === 1 ? `${mantido} natural` : null);

/** Marca como "mantido" só o primeiro dado com o valor mantido (dois dados iguais não ficam ambos em negrito). */
function d20s(dados: number[], mantido: number): DadoDaLinha[] {
  const indice = dados.indexOf(mantido);
  return dados.map((valor, i) => ({ valor, mantido: i === indice }));
}

/** Lê só campos que já existem nas 3 respostas de rolagem (a spec avisa que o `detalhe` acompanha os DTOs). */
export function detalhesDe(r: RolagemDaMesa): Detalhes {
  switch (r.tipo) {
    case 'TESTE':
      return {
        dados: d20s(r.detalhe.dados, r.detalhe.mantido),
        natural: naturalDe(r.detalhe.mantido),
        critico: false,
        dano: null,
        bonus: doBonus(r.detalhe.bonus),
      };
    case 'ATAQUE':
      return {
        dados: d20s(r.detalhe.ataque.dados, r.detalhe.ataque.mantido),
        natural: naturalDe(r.detalhe.ataque.mantido),
        critico: r.detalhe.critico,
        dano: `dano ${r.detalhe.dano.total} (${r.detalhe.dano.expressao})`,
        bonus: doBonus(r.detalhe.ataque.bonus),
      };
    case 'LIVRE':
      return {
        dados: r.detalhe.dados.map((d) => ({ valor: d.valor, mantido: false })),
        natural: null,
        critico: false,
        dano: null,
        bonus: doBonus(r.detalhe.bonus),
      };
  }
}

/**
 * Painel lateral com as rolagens da campanha: as 50 últimas (GET) e as novas ao vivo (tópico), juntas pelo
 * `id`. Quem o abre cuida só de mostrá-lo e de ouvir `fechar`. Ao trocar o `campanhaId` a lista é refeita:
 * a inscrição e o GET da campanha antiga são cancelados, então nada dela aparece na nova.
 */
@Component({
  selector: 'app-painel-rolagens',
  templateUrl: './painel-rolagens.html',
  styleUrl: './painel-rolagens.scss',
  host: { '(document:keydown.escape)': 'fechar.emit()' },
})
export class PainelRolagens {
  private readonly rolagensApi = inject(RolagensApi);
  private readonly campanhaApi = inject(CampanhaApi);
  private readonly mesa = inject(MesaAoVivo);

  readonly campanhaId = input.required<number>();
  readonly fechar = output<void>();

  readonly rolagens = signal<RolagemDaMesa[]>([]);
  readonly carregando = signal(true);
  readonly carregandoMais = signal(false);
  readonly temMais = signal(false);
  readonly erro = signal<ApiError | null>(null);
  /** O mestre da campanha, para o "(rolado pelo mestre)"; sem ele a linha cai no formato comum. */
  private readonly mestreId = signal<number | null>(null);
  /** Relógio das horas relativas: o painel aberto continua mostrando "agora" → "há 1 min". */
  private readonly agora = signal(Date.now());

  readonly estado = this.mesa.estado;
  readonly ehVazio = computed(() => !this.carregando() && this.rolagens().length === 0);
  protected readonly titulo = viewChild<ElementRef<HTMLElement>>('titulo');

  constructor() {
    effect((limpar) => {
      const id = this.campanhaId();
      const assinaturas = untracked(() => this.iniciar(id));
      limpar(() => assinaturas.unsubscribe());
    });
    const relogio = setInterval(() => this.agora.set(Date.now()), 30_000);
    inject(DestroyRef).onDestroy(() => clearInterval(relogio));
    // Foco no título ao abrir: o leitor de tela anuncia o diálogo e Esc/Tab partem de dentro dele.
    afterNextRender(() => this.titulo()?.nativeElement.focus());
  }

  /** Zera a lista e liga as fontes. O ao vivo entra antes do GET, para não perder o que chegar enquanto ele volta. */
  private iniciar(campanhaId: number): Subscription {
    this.rolagens.set([]);
    this.carregando.set(true);
    this.carregandoMais.set(false);
    this.temMais.set(false);
    this.erro.set(null);
    this.mestreId.set(null);

    const assinaturas = new Subscription();
    assinaturas.add(
      this.mesa.rolagens$(campanhaId).subscribe((rolagem) => {
        // Defesa: o tópico é da campanha, mas nada de outra campanha pode entrar nesta lista.
        if (rolagem.campanhaId === campanhaId) {
          this.rolagens.update((atuais) => juntarRolagens(atuais, [rolagem]));
        }
      }),
    );
    assinaturas.add(
      this.rolagensApi.historico(campanhaId).subscribe({
        next: (pagina) => {
          this.rolagens.update((atuais) => juntarRolagens(atuais, pagina));
          this.temMais.set(pagina.length >= TAMANHO_DA_PAGINA);
          this.carregando.set(false);
        },
        error: (erro: ApiError) => {
          this.erro.set(erro);
          this.carregando.set(false);
        },
      }),
    );
    assinaturas.add(
      this.campanhaApi
        .buscar(campanhaId)
        .pipe(catchError(() => of(null)))
        .subscribe((campanha) => this.mestreId.set(campanha?.mestre.id ?? null)),
    );
    return assinaturas;
  }

  carregarMais(): void {
    const lista = this.rolagens();
    if (this.carregandoMais() || lista.length === 0) {
      return;
    }
    const campanhaId = this.campanhaId();
    const antes = Math.min(...lista.map((r) => r.id));
    this.carregandoMais.set(true);
    this.erro.set(null);
    this.rolagensApi.historico(campanhaId, antes).subscribe({
      next: (pagina) => {
        if (campanhaId !== this.campanhaId()) {
          return; // a pessoa trocou de campanha enquanto a página vinha
        }
        this.rolagens.update((atuais) => juntarRolagens(atuais, pagina));
        this.temMais.set(pagina.length >= TAMANHO_DA_PAGINA);
        this.carregandoMais.set(false);
      },
      error: (erro: ApiError) => {
        this.erro.set(erro);
        this.carregandoMais.set(false);
      },
    });
  }

  protected quem(r: RolagemDaMesa): string {
    return quemRolou(r, this.mestreId());
  }

  protected hora(r: RolagemDaMesa): string {
    return horaRelativa(r.criadaEm, this.agora());
  }

  protected detalhes(r: RolagemDaMesa): Detalhes {
    return detalhesDe(r);
  }
}
