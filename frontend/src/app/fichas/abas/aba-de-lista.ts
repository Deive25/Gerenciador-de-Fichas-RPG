import { ChangeDetectorRef, ElementRef, Injector, inject, signal } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiError } from '../../core/api-error';
import {
  aplicarErrosDoServidor,
  focarPrimeiroInvalido,
  focarPrimeiroInvalidoDepois,
  mensagemDoControle,
} from '../sistemas/comum/formulario-comum';

/**
 * Esqueleto das abas que editam uma lista de itens da ficha (habilidades, itens do inventário…):
 * abrir e fechar o formulário, salvar (criar ou atualizar) e remover, com o envio duplo bloqueado e os
 * erros do servidor nos campos. Cada aba fornece só o que muda: o formulário, a lista atual e as
 * chamadas à API.
 *
 * Não leva decorator: o `inject()` dos campos roda dentro da construção do componente que herda, e os
 * `input()`/`output()` ficam nas subclasses, onde o compilador do Angular os enxerga.
 */
export abstract class AbaDeLista<T extends { id: number }> {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly cdr = inject(ChangeDetectorRef);

  protected readonly editando = signal<number | 'novo' | null>(null);
  protected readonly salvando = signal(false);
  /** Erro que não é de um campo (ex.: servidor indisponível, falha ao remover). */
  protected readonly erroGeral = signal<ApiError | null>(null);
  protected readonly mensagem = mensagemDoControle;

  protected abstract readonly form: AbstractControl;
  /** A lista atual de itens da ficha. */
  protected abstract itens(): readonly T[];
  /** Põe no formulário os valores do item, ou os iniciais quando não há item (novo). */
  protected abstract preencher(item: T | undefined): void;
  /** Monta a entrada com os valores do formulário e chama a API (criar em `'novo'`, atualizar no id). */
  protected abstract gravar(destino: number | 'novo'): Observable<T>;
  protected abstract excluir(item: T): Observable<void>;
  /** Avisa a lista nova; o detalhe a coloca na ficha. */
  protected abstract emitir(lista: T[]): void;

  /** Abrir ou trocar o formulário: o formulário começa limpo (novo) ou com os valores do item. */
  protected aoMudarEdicao(destino: number | 'novo' | null): void {
    this.editando.set(destino);
    this.erroGeral.set(null);
    this.preencher(typeof destino === 'number' ? this.itens().find((i) => i.id === destino) : undefined);
  }

  protected cancelar(): void {
    this.aoMudarEdicao(null);
  }

  protected salvar(): void {
    const destino = this.editando();
    if (this.salvando() || destino === null) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      focarPrimeiroInvalido(this.host.nativeElement);
      return;
    }
    const requisicao = this.gravar(destino);

    this.erroGeral.set(null);
    this.salvando.set(true);
    requisicao.subscribe({
      next: (gravado) => {
        this.salvando.set(false);
        const atual = this.itens();
        this.emitir(destino === 'novo' ? [...atual, gravado] : atual.map((i) => (i.id === gravado.id ? gravado : i)));
        this.aoMudarEdicao(null);
      },
      error: (erro: ApiError) => {
        this.salvando.set(false);
        if (aplicarErrosDoServidor(this.form, erro)) {
          focarPrimeiroInvalidoDepois(this.host.nativeElement, this.injector, this.cdr);
        } else {
          this.erroGeral.set(erro);
        }
      },
    });
  }

  protected remover(item: T): void {
    if (this.salvando()) {
      return;
    }
    this.erroGeral.set(null);
    this.salvando.set(true);
    this.excluir(item).subscribe({
      next: () => {
        this.salvando.set(false);
        this.emitir(this.itens().filter((i) => i.id !== item.id));
      },
      error: (erro: ApiError) => {
        this.salvando.set(false);
        this.erroGeral.set(erro);
      },
    });
  }
}
