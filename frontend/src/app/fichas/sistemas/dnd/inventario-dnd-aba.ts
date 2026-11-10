import { Component, computed, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';
import { AbaDeLista } from '../../abas/aba-de-lista';
import { ListaEditavel } from '../../componentes/lista-editavel/lista-editavel';
import { FichaApi } from '../../ficha-api';
import { FichaDnd, ItemDnd, ItemDndEntrada, Moedas } from '../../ficha.model';
import { INTEIRO, decimal, faixa, paraDecimal, paraNumero, textoDe } from '../comum/formulario-comum';
import { MoedasDnd } from './moedas-dnd';
import { capacidadeDeCarga, formatarPeso, pesoTotal } from './regras-dnd';

const NOME_MAXIMO = 255;
const DESCRICAO_MAXIMA = 4000;
const PESO_MAXIMO = 99999.99;

/**
 * Aba Inventário de D&D: o peso total contra a capacidade (FOR × 7,5 kg), as moedas e a lista de itens.
 * O salvar e o remover vêm da AbaDeLista; a conta de peso são funções puras de regras-dnd. A cada gravação
 * a aba avisa a lista nova (`atualizada`) ou as moedas novas (`moedasAtualizadas`), e o detalhe as troca
 * na ficha sem recarregar.
 */
@Component({
  selector: 'app-inventario-dnd-aba',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, ListaEditavel, MoedasDnd],
  templateUrl: './inventario-dnd-aba.html',
  styleUrl: './inventario-dnd-aba.scss',
})
export class InventarioDndAba extends AbaDeLista<ItemDnd> {
  private readonly api = inject(FichaApi);

  readonly ficha = input.required<FichaDnd>();
  /** A lista de itens depois de uma gravação; o detalhe a coloca na ficha. */
  readonly atualizada = output<ItemDnd[]>();
  /** As moedas depois de gravadas; o detalhe as coloca na ficha. */
  readonly moedasAtualizadas = output<Moedas>();

  protected readonly total = computed(() => pesoTotal(this.ficha().itens));
  protected readonly capacidade = computed(() => capacidadeDeCarga(this.ficha().atributos.FOR));
  /** Na capacidade exata não há aviso: só quando passa. */
  protected readonly acima = computed(() => this.total() > this.capacidade());
  protected readonly formatarPeso = formatarPeso;

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(NOME_MAXIMO)]],
    quantidade: ['1', [Validators.required, Validators.pattern(INTEIRO), faixa(1)]],
    peso: ['0', [Validators.required, decimal(PESO_MAXIMO)]],
    equipado: [false],
    descricao: ['', [Validators.maxLength(DESCRICAO_MAXIMA)]],
  });

  protected readonly tituloDe = (i: ItemDnd) => i.nome;

  protected itens(): readonly ItemDnd[] {
    return this.ficha().itens;
  }

  protected preencher(item: ItemDnd | undefined): void {
    this.form.reset({
      nome: item?.nome ?? '',
      quantidade: textoDe(item?.quantidade ?? 1),
      peso: textoDe(item?.peso ?? 0),
      equipado: item?.equipado ?? false,
      descricao: item?.descricao ?? '',
    });
  }

  protected gravar(destino: number | 'novo'): Observable<ItemDnd> {
    const v = this.form.getRawValue();
    // Só aqui o peso vira número: o validador `decimal` já aprovou o texto (o paraDecimal não valida).
    const entrada: ItemDndEntrada = {
      nome: v.nome.trim(),
      quantidade: paraNumero(v.quantidade) as number,
      peso: paraDecimal(v.peso) as number,
      equipado: v.equipado,
      descricao: v.descricao.trim() === '' ? null : v.descricao,
    };
    const fichaId = this.ficha().id;
    return destino === 'novo'
      ? this.api.criarItem<ItemDnd>(fichaId, entrada)
      : this.api.atualizarItem<ItemDnd>(fichaId, destino, entrada);
  }

  protected excluir(item: ItemDnd): Observable<void> {
    return this.api.excluirItem(this.ficha().id, item.id);
  }

  protected emitir(lista: ItemDnd[]): void {
    this.atualizada.emit(lista);
  }
}
