import { Component, computed, inject, input, output, signal } from '@angular/core';
import { CampanhaApi } from '../../campanhas/campanha-api';
import { CampanhaResumo, Referencia } from '../../campanhas/campanha.model';
import { ApiError } from '../../core/api-error';
import { FichaApi } from '../ficha-api';
import { Ficha } from '../ficha.model';

/**
 * Seletor "Campanha" no cabeçalho da ficha (só o dono vê): leva a ficha a uma campanha de que ele
 * participa ou a tira de uma ("Sem campanha"). Cada escolha grava na hora, sem botão de salvar:
 * é a mesma rota `PUT /fichas/{id}/campanha`, que não leva versão.
 */
@Component({
  selector: 'app-seletor-campanha',
  templateUrl: './seletor-campanha.html',
  styleUrl: './seletor-campanha.scss',
})
export class SeletorCampanha {
  private readonly campanhas = inject(CampanhaApi);
  private readonly fichas = inject(FichaApi);

  readonly ficha = input.required<Ficha>();
  /** A ficha como o servidor devolveu depois da troca. */
  readonly atualizada = output<Ficha>();

  protected readonly lista = signal<CampanhaResumo[]>([]);
  protected readonly trocando = signal(false);
  protected readonly erro = signal<ApiError | null>(null);
  protected readonly aviso = signal<string | null>(null);

  /** As campanhas da pessoa; a da ficha entra mesmo que a lista não a traga (nunca some a opção em uso). */
  protected readonly opcoes = computed<Referencia[]>(() => {
    const todas: Referencia[] = this.lista().map(({ id, nome }) => ({ id, nome }));
    const atual = this.ficha().campanha;
    return atual && !todas.some((c) => c.id === atual.id) ? [...todas, atual] : todas;
  });

  protected readonly valorAtual = computed(() => String(this.ficha().campanha?.id ?? ''));

  constructor() {
    this.campanhas.listar().subscribe({
      next: (lista) => this.lista.set(lista),
      error: (erro: ApiError) => this.erro.set(erro),
    });
  }

  protected trocar(evento: Event): void {
    const seletor = evento.target as HTMLSelectElement;
    if (this.trocando()) {
      return;
    }
    const campanhaId = seletor.value === '' ? null : Number(seletor.value);
    this.erro.set(null);
    this.aviso.set(null);
    this.trocando.set(true);
    this.fichas.trocarCampanha(this.ficha().id, campanhaId).subscribe({
      next: (ficha) => {
        this.trocando.set(false);
        this.aviso.set(campanhaId === null ? 'A ficha ficou sem campanha.' : 'Campanha alterada.');
        this.atualizada.emit(ficha);
      },
      error: (erro: ApiError) => {
        this.trocando.set(false);
        this.erro.set(erro);
        seletor.value = this.valorAtual();
      },
    });
  }
}
