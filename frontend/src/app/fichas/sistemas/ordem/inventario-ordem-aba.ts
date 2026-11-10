import { Component, computed, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';
import { AbaDeLista } from '../../abas/aba-de-lista';
import { ListaEditavel } from '../../componentes/lista-editavel/lista-editavel';
import { FichaApi } from '../../ficha-api';
import { FichaOrdem, ItemOrdem, ItemOrdemEntrada } from '../../ficha.model';
import { INTEIRO, faixa, paraNumero, textoDe } from '../comum/formulario-comum';
import { ROTULO_CATEGORIA, espacosUsados, limiteDeCarga, rotuloEspacos } from './catalogo-ordem';

const NOME_MAXIMO = 255;
const DESCRICAO_MAXIMA = 4000;

/** As categorias de 0 a IV, com o valor numérico que a API recebe (0 é válido). */
const CATEGORIAS = ROTULO_CATEGORIA.map((rotulo, valor) => ({ valor, rotulo }));

/**
 * Aba Inventário de Ordem Paranormal: a carga usada contra o limite (5 × FOR) e a lista de itens.
 * O salvar e o remover vêm da AbaDeLista; a conta de carga são funções puras do catálogo. A cada gravação
 * a aba avisa a lista nova (`atualizada`) e o detalhe troca `ficha.itens` sem recarregar a ficha.
 */
@Component({
  selector: 'app-inventario-ordem-aba',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, ListaEditavel],
  templateUrl: './inventario-ordem-aba.html',
  styleUrl: './inventario-ordem-aba.scss',
})
export class InventarioOrdemAba extends AbaDeLista<ItemOrdem> {
  private readonly api = inject(FichaApi);

  readonly ficha = input.required<FichaOrdem>();
  /** A lista de itens depois de uma gravação; o detalhe a coloca na ficha. */
  readonly atualizada = output<ItemOrdem[]>();

  protected readonly categorias = CATEGORIAS;
  protected readonly usado = computed(() => espacosUsados(this.ficha().itens));
  protected readonly limite = computed(() => limiteDeCarga(this.ficha().atributos.FOR));
  /** No limite exato não há aviso: só quando passa. */
  protected readonly acima = computed(() => this.usado() > this.limite());

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(NOME_MAXIMO)]],
    // O 0 é uma categoria válida: o Validators.required só barra null e texto vazio, não o número 0.
    categoria: [0, [Validators.required]],
    espacos: ['1', [Validators.required, Validators.pattern(INTEIRO), faixa(0)]],
    quantidade: ['1', [Validators.required, Validators.pattern(INTEIRO), faixa(1)]],
    descricao: ['', [Validators.maxLength(DESCRICAO_MAXIMA)]],
  });

  protected readonly tituloDe = (i: ItemOrdem) => i.nome;
  protected readonly rotuloEspacos = rotuloEspacos;

  protected rotuloDaCategoria(categoria: number): string {
    return ROTULO_CATEGORIA[categoria] ?? String(categoria);
  }

  protected itens(): readonly ItemOrdem[] {
    return this.ficha().itens;
  }

  protected preencher(item: ItemOrdem | undefined): void {
    this.form.reset({
      nome: item?.nome ?? '',
      categoria: item?.categoria ?? 0,
      espacos: textoDe(item?.espacos ?? 1),
      quantidade: textoDe(item?.quantidade ?? 1),
      descricao: item?.descricao ?? '',
    });
  }

  protected gravar(destino: number | 'novo'): Observable<ItemOrdem> {
    const v = this.form.getRawValue();
    const entrada: ItemOrdemEntrada = {
      nome: v.nome.trim(),
      categoria: v.categoria,
      espacos: paraNumero(v.espacos) as number,
      quantidade: paraNumero(v.quantidade) as number,
      descricao: v.descricao.trim() === '' ? null : v.descricao,
    };
    const fichaId = this.ficha().id;
    return destino === 'novo'
      ? this.api.criarItem<ItemOrdem>(fichaId, entrada)
      : this.api.atualizarItem<ItemOrdem>(fichaId, destino, entrada);
  }

  protected excluir(item: ItemOrdem): Observable<void> {
    return this.api.excluirItem(this.ficha().id, item.id);
  }

  protected emitir(lista: ItemOrdem[]): void {
    this.atualizada.emit(lista);
  }
}
