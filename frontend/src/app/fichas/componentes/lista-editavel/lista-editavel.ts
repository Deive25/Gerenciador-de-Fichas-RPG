import { NgTemplateOutlet } from '@angular/common';
import { Component, TemplateRef, contentChild, input, model, output, signal } from '@angular/core';

/**
 * Lista com adicionar, editar no lugar e remover com confirmação. É só a interface: quem fala com a
 * API é quem usa. O uso projeta dois templates, `#visualizacao` (o cartão do item) e `#formulario`
 * (recebe o item, ou `null` ao adicionar), e fecha o formulário com `editando.set(null)`.
 * O 2.1h+ reaproveita para inventário, ataques, magias e rituais.
 */
@Component({
  selector: 'app-lista-editavel',
  imports: [NgTemplateOutlet],
  templateUrl: './lista-editavel.html',
  styleUrl: './lista-editavel.scss',
})
export class ListaEditavel<T extends { id: number }> {
  readonly itens = input.required<readonly T[]>();
  /** Ex.: "habilidade" → "Adicionar habilidade". */
  readonly nomeDoItem = input.required<string>();
  /** Texto da lista vazia, já com a concordância certa (ex.: "Nenhuma habilidade ainda."). */
  readonly vazio = input.required<string>();
  readonly tituloDe = input.required<(item: T) => string>();
  /** Bloqueia os botões enquanto o uso grava na API. */
  readonly salvando = input(false);

  /** O id do item em edição, 'novo' para o formulário de adicionar, ou null; só um fica aberto. */
  readonly editando = model<number | 'novo' | null>(null);
  /** Emitido só depois do "Sim, remover". */
  readonly remover = output<T>();

  protected readonly visualizacao = contentChild<TemplateRef<{ $implicit: T }>>('visualizacao');
  protected readonly formulario = contentChild<TemplateRef<{ $implicit: T | null }>>('formulario');

  /** Id do item que está pedindo confirmação de remoção. */
  protected readonly confirmando = signal<number | null>(null);

  protected adicionar(): void {
    this.confirmando.set(null);
    this.editando.set('novo');
  }

  protected editar(item: T): void {
    this.confirmando.set(null);
    this.editando.set(item.id);
  }

  protected confirmar(item: T): void {
    this.confirmando.set(null);
    this.remover.emit(item);
  }
}
