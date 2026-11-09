import { Component, input, output } from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FichaOrdem, PedidoTeste } from '../../ficha.model';
import {
  ATRIBUTOS_ORDEM,
  NOME_GRAU,
  PERICIAS_ORDEM,
  SELO_GRAU,
  bonusDoGrau,
  defesa,
  rotuloClasse,
} from './catalogo-ordem';

/**
 * Corpo da ficha de Ordem Paranormal: identidade, atributos e perícias. Só desenha e avisa o que o
 * jogador quer rolar; quem chama a API e mostra o resultado é a tela da ficha (FichaDetalhe).
 */
@Component({
  selector: 'app-ficha-ordem',
  imports: [MatTooltipModule],
  templateUrl: './ficha-ordem.html',
  styleUrl: './ficha-ordem.scss',
})
export class FichaOrdemCorpo {
  readonly ficha = input.required<FichaOrdem>();
  /** Há uma rolagem em andamento: todos os "Rolar" ficam desabilitados. */
  readonly rolando = input(false);
  readonly rolar = output<PedidoTeste>();

  readonly atributos = ATRIBUTOS_ORDEM;
  readonly pericias = PERICIAS_ORDEM;
  readonly seloGrau = SELO_GRAU;
  readonly nomeGrau = NOME_GRAU;
  readonly bonusDoGrau = bonusDoGrau;
  readonly defesa = defesa;
  readonly rotuloClasse = rotuloClasse;
}
