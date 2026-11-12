import { Component, computed, input, output, signal } from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SeletorDeModo } from '../../componentes/seletor-de-modo';
import { AlvoTeste, FichaDnd, ModoTeste, PedidoTeste } from '../../ficha.model';
import {
  ATRIBUTOS_DND,
  NOMES_EDICAO,
  NOME_GRAU_DND,
  PERICIAS_DND,
  SELO_GRAU_DND,
  rotuloEspecie,
} from './catalogo-dnd';
import { bonusPericia, bonusSalvaguarda, comSinal, modificador, proficiencia } from './regras-dnd';

/**
 * Corpo da ficha de D&D (as duas edições): identidade, atributos, salvaguardas e as 18 perícias.
 * O front só calcula os bônus para mostrar; quem rola os dados é o servidor. O modo
 * (Normal/Vantagem/Desvantagem) vale para a próxima rolagem e volta a Normal depois dela.
 */
@Component({
  selector: 'app-ficha-dnd',
  imports: [MatTooltipModule, SeletorDeModo],
  templateUrl: './ficha-dnd.html',
  styleUrl: './ficha-dnd.scss',
})
export class FichaDndCorpo {
  readonly ficha = input.required<FichaDnd>();
  /** Há uma rolagem em andamento: todos os "Rolar" ficam desabilitados. */
  readonly rolando = input(false);
  readonly rolar = output<PedidoTeste>();

  readonly modo = signal<ModoTeste>('NORMAL');

  readonly atributos = ATRIBUTOS_DND;
  readonly pericias = PERICIAS_DND;
  readonly seloGrau = SELO_GRAU_DND;
  readonly nomeGrau = NOME_GRAU_DND;
  readonly nomeEdicao = NOMES_EDICAO;
  readonly rotuloEspecie = rotuloEspecie;
  readonly comSinal = comSinal;
  readonly modificador = modificador;
  readonly bonusPericia = bonusPericia;
  readonly bonusSalvaguarda = bonusSalvaguarda;

  readonly proficiencia = computed(() => proficiencia(this.ficha().nivel));

  /** Pede a rolagem com o modo escolhido (sem `modo` no Normal) e volta o seletor a Normal. */
  pedir(alvo: AlvoTeste, codigo: string): void {
    const modo = this.modo();
    this.rolar.emit(modo === 'NORMAL' ? { alvo, codigo } : { alvo, codigo, modo });
    this.modo.set('NORMAL');
  }
}
