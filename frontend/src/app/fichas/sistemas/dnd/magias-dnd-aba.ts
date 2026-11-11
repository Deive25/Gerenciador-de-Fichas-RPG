import { Component, computed, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';
import { AbaDeLista } from '../../abas/aba-de-lista';
import { ListaEditavel } from '../../componentes/lista-editavel/lista-editavel';
import { FichaApi } from '../../ficha-api';
import { Espacos, FichaDnd, MagiaDnd, MagiaDndEntrada } from '../../ficha.model';
import { textoOuNulo } from '../comum/formulario-comum';
import { ATRIBUTOS_DND } from './catalogo-dnd';
import { EspacosDnd } from './espacos-dnd';
import { ataqueMagico, cdDeMagia, comSinal, ordenarMagias, rotuloNivelMagia } from './regras-dnd';

const NOME_MAXIMO = 255;
const TEXTO_MAXIMO = 255;
const DESCRICAO_MAXIMA = 4000;

/** Truque e os níveis de 1 a 9, com o valor numérico que a API recebe (0 é válido). */
const NIVEIS = Array.from({ length: 10 }, (_, valor) => ({ valor, rotulo: rotuloNivelMagia(valor) }));

/**
 * Aba Magias de D&D: o atributo de conjuração com a CD e o ataque mágico, os espaços por nível e a lista
 * de magias. O salvar e o remover vêm da AbaDeLista; as contas são funções puras de regras-dnd. A cada
 * gravação a aba avisa a lista nova (`atualizada`) ou os espaços novos (`espacosAtualizados`), e o detalhe
 * os troca na ficha sem recarregar.
 */
@Component({
  selector: 'app-magias-dnd-aba',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule, ListaEditavel, EspacosDnd],
  templateUrl: './magias-dnd-aba.html',
  styleUrl: './magias-dnd-aba.scss',
})
export class MagiasDndAba extends AbaDeLista<MagiaDnd> {
  private readonly api = inject(FichaApi);

  readonly ficha = input.required<FichaDnd>();
  /** A lista de magias depois de uma gravação; o detalhe a coloca na ficha. */
  readonly atualizada = output<MagiaDnd[]>();
  /** Os 9 níveis de espaços depois de gravados; o detalhe os coloca na ficha. */
  readonly espacosAtualizados = output<Espacos>();

  protected readonly niveis = NIVEIS;
  protected readonly rotuloNivel = rotuloNivelMagia;
  protected readonly comSinal = comSinal;

  /** Valor do atributo de conjuração, ou null quando a ficha não escolheu um. */
  private readonly valorDoAtributo = computed(() => {
    const atributo = this.ficha().atributoConjuracao;
    return atributo === null ? null : this.ficha().atributos[atributo];
  });
  protected readonly nomeDoAtributo = computed(() => {
    const atributo = this.ficha().atributoConjuracao;
    return atributo === null ? null : (ATRIBUTOS_DND.find((a) => a.codigo === atributo)?.nome ?? atributo);
  });
  protected readonly cd = computed(() => cdDeMagia(this.ficha().nivel, this.valorDoAtributo()));
  protected readonly ataque = computed(() => ataqueMagico(this.ficha().nivel, this.valorDoAtributo()));

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(NOME_MAXIMO)]],
    // O 0 (truque) é um nível válido: o Validators.required só barra null e texto vazio, não o número 0.
    nivel: [1, [Validators.required]],
    escola: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    tempo: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    alcance: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    componentes: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    duracao: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    concentracao: [false],
    ritual: [false],
    preparada: [false],
    descricao: ['', [Validators.maxLength(DESCRICAO_MAXIMA)]],
  });

  protected readonly tituloDe = (m: MagiaDnd) => m.nome;

  /** "Tempo: 1 ação · Alcance: 45 m…": só o que a magia tem. */
  protected detalhes(m: MagiaDnd): string {
    return [
      m.tempo && `Tempo: ${m.tempo}`,
      m.alcance && `Alcance: ${m.alcance}`,
      m.componentes && `Componentes: ${m.componentes}`,
      m.duracao && `Duração: ${m.duracao}`,
    ]
      .filter((parte) => !!parte)
      .join(' · ');
  }

  protected itens(): readonly MagiaDnd[] {
    return this.ficha().magias;
  }

  protected preencher(magia: MagiaDnd | undefined): void {
    this.form.reset({
      nome: magia?.nome ?? '',
      nivel: magia?.nivel ?? 1,
      escola: magia?.escola ?? '',
      tempo: magia?.tempo ?? '',
      alcance: magia?.alcance ?? '',
      componentes: magia?.componentes ?? '',
      duracao: magia?.duracao ?? '',
      concentracao: magia?.concentracao ?? false,
      ritual: magia?.ritual ?? false,
      preparada: magia?.preparada ?? false,
      descricao: magia?.descricao ?? '',
    });
  }

  protected gravar(destino: number | 'novo'): Observable<MagiaDnd> {
    const v = this.form.getRawValue();
    const entrada: MagiaDndEntrada = {
      nome: v.nome.trim(),
      nivel: v.nivel,
      escola: textoOuNulo(v.escola),
      tempo: textoOuNulo(v.tempo),
      alcance: textoOuNulo(v.alcance),
      componentes: textoOuNulo(v.componentes),
      duracao: textoOuNulo(v.duracao),
      concentracao: v.concentracao,
      ritual: v.ritual,
      preparada: v.preparada,
      descricao: v.descricao.trim() === '' ? null : v.descricao,
    };
    const fichaId = this.ficha().id;
    return destino === 'novo' ? this.api.criarMagia(fichaId, entrada) : this.api.atualizarMagia(fichaId, destino, entrada);
  }

  protected excluir(magia: MagiaDnd): Observable<void> {
    return this.api.excluirMagia(this.ficha().id, magia.id);
  }

  /** A lista volta ordenada por nível e criação, como a API: a magia editada não pula de lugar só na tela. */
  protected emitir(lista: MagiaDnd[]): void {
    this.atualizada.emit(ordenarMagias(lista));
  }
}
