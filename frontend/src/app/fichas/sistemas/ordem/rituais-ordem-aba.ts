import { Component, computed, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';
import { AbaDeLista } from '../../abas/aba-de-lista';
import { ListaEditavel } from '../../componentes/lista-editavel/lista-editavel';
import { FichaApi } from '../../ficha-api';
import { ElementoRitual, FichaOrdem, RitualOrdem, RitualOrdemEntrada } from '../../ficha.model';
import { textoOuNulo } from '../comum/formulario-comum';
import {
  CIRCULOS_RITUAL,
  ELEMENTOS_RITUAL,
  custoEmPe,
  dtDeRitual,
  ordenarRituais,
  rotuloCirculo,
  rotuloElemento,
} from './catalogo-ordem';

const NOME_MAXIMO = 255;
const TEXTO_MAXIMO = 255;
const DESCRICAO_MAXIMA = 4000;

const CIRCULOS = CIRCULOS_RITUAL.map((valor) => ({ valor, rotulo: rotuloCirculo(valor) }));

/**
 * Aba Rituais de Ordem Paranormal: a lista de rituais, cada um com o custo em PE (pelo círculo) e a DT
 * (10 + NEX/5 + Presença). O salvar e o remover vêm da AbaDeLista; as contas são funções puras do catálogo.
 * A cada gravação a aba avisa a lista nova (`atualizada`) e o detalhe troca `ficha.rituais` sem recarregar.
 */
@Component({
  selector: 'app-rituais-ordem-aba',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, ListaEditavel],
  templateUrl: './rituais-ordem-aba.html',
  styleUrl: './rituais-ordem-aba.scss',
})
export class RituaisOrdemAba extends AbaDeLista<RitualOrdem> {
  private readonly api = inject(FichaApi);

  readonly ficha = input.required<FichaOrdem>();
  /** A lista de rituais depois de uma gravação; o detalhe a coloca na ficha. */
  readonly atualizada = output<RitualOrdem[]>();

  protected readonly circulos = CIRCULOS;
  protected readonly elementos = ELEMENTOS_RITUAL;
  /** A DT é a mesma para todos os rituais da ficha: depende só do NEX e da Presença. */
  protected readonly dt = computed(() => dtDeRitual(this.ficha().nex, this.ficha().atributos.PRE));

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(NOME_MAXIMO)]],
    circulo: [1, [Validators.required]],
    elemento: ['SANGUE' as ElementoRitual, [Validators.required]],
    execucao: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    alcance: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    alvo: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    duracao: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    resistencia: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    descricao: ['', [Validators.maxLength(DESCRICAO_MAXIMA)]],
  });

  protected readonly tituloDe = (r: RitualOrdem) => r.nome;
  protected readonly rotuloCirculo = rotuloCirculo;
  protected readonly rotuloElemento = rotuloElemento;
  protected readonly custoEmPe = custoEmPe;

  /** "Execução: Padrão · Alcance: Curto…": só o que o ritual tem. */
  protected detalhes(r: RitualOrdem): string {
    return [
      r.execucao && `Execução: ${r.execucao}`,
      r.alcance && `Alcance: ${r.alcance}`,
      r.alvo && `Alvo: ${r.alvo}`,
      r.duracao && `Duração: ${r.duracao}`,
      r.resistencia && `Resistência: ${r.resistencia}`,
    ]
      .filter((parte) => !!parte)
      .join(' · ');
  }

  protected itens(): readonly RitualOrdem[] {
    return this.ficha().rituais;
  }

  protected preencher(ritual: RitualOrdem | undefined): void {
    this.form.reset({
      nome: ritual?.nome ?? '',
      circulo: ritual?.circulo ?? 1,
      elemento: ritual?.elemento ?? 'SANGUE',
      execucao: ritual?.execucao ?? '',
      alcance: ritual?.alcance ?? '',
      alvo: ritual?.alvo ?? '',
      duracao: ritual?.duracao ?? '',
      resistencia: ritual?.resistencia ?? '',
      descricao: ritual?.descricao ?? '',
    });
  }

  protected gravar(destino: number | 'novo'): Observable<RitualOrdem> {
    const v = this.form.getRawValue();
    const entrada: RitualOrdemEntrada = {
      nome: v.nome.trim(),
      circulo: v.circulo,
      elemento: v.elemento,
      execucao: textoOuNulo(v.execucao),
      alcance: textoOuNulo(v.alcance),
      alvo: textoOuNulo(v.alvo),
      duracao: textoOuNulo(v.duracao),
      resistencia: textoOuNulo(v.resistencia),
      descricao: v.descricao.trim() === '' ? null : v.descricao,
    };
    const fichaId = this.ficha().id;
    return destino === 'novo' ? this.api.criarRitual(fichaId, entrada) : this.api.atualizarRitual(fichaId, destino, entrada);
  }

  protected excluir(ritual: RitualOrdem): Observable<void> {
    return this.api.excluirRitual(this.ficha().id, ritual.id);
  }

  /** A lista volta ordenada por círculo e criação, como a API: o ritual editado não pula de lugar só na tela. */
  protected emitir(lista: RitualOrdem[]): void {
    this.atualizada.emit(ordenarRituais(lista));
  }
}
