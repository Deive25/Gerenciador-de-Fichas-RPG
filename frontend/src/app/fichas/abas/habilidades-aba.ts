import { Component, computed, inject, input, output } from '@angular/core';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';
import { ListaEditavel } from '../componentes/lista-editavel/lista-editavel';
import { FichaApi } from '../ficha-api';
import { Ficha, Habilidade, HabilidadeEntrada, TipoHabilidade } from '../ficha.model';
import { rotuloEspecie } from '../sistemas/dnd/catalogo-dnd';
import { AbaDeLista } from './aba-de-lista';

interface OpcaoTipo {
  valor: TipoHabilidade;
  rotulo: string;
}

const TIPOS_ORDEM: readonly OpcaoTipo[] = [
  { valor: 'CLASSE', rotulo: 'Classe' },
  { valor: 'TRILHA', rotulo: 'Trilha' },
  { valor: 'ORIGEM', rotulo: 'Origem' },
  { valor: 'PARANORMAL', rotulo: 'Poder paranormal' },
  { valor: 'OUTRA', rotulo: 'Outra' },
];

const NOME_MAXIMO = 255;
const DESCRICAO_MAXIMA = 4000;

/**
 * Aba Habilidades: o ListaEditavel cuida da interface (adicionar, editar, remover) e esta aba
 * fala com a API e mantém o formulário. O salvar e o remover vêm da AbaDeLista. A cada gravação ela
 * avisa a lista nova (`atualizada`) e o detalhe troca `ficha.habilidades` sem recarregar a ficha.
 */
@Component({
  selector: 'app-habilidades-aba',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, ListaEditavel],
  templateUrl: './habilidades-aba.html',
  styleUrl: './habilidades-aba.scss',
})
export class HabilidadesAba extends AbaDeLista<Habilidade> {
  private readonly api = inject(FichaApi);

  readonly ficha = input.required<Ficha>();
  /** A lista de habilidades depois de uma gravação; o detalhe a coloca na ficha. */
  readonly atualizada = output<Habilidade[]>();

  /** Os tipos do sistema da ficha; em D&D o rótulo de ESPECIE segue a edição (Raça ou Espécie). */
  protected readonly tipos = computed<readonly OpcaoTipo[]>(() => {
    const ficha = this.ficha();
    if (ficha.sistema === 'ORDEM_PARANORMAL') {
      return TIPOS_ORDEM;
    }
    return [
      { valor: 'CLASSE', rotulo: 'Classe' },
      { valor: 'SUBCLASSE', rotulo: 'Subclasse' },
      { valor: 'ESPECIE', rotulo: rotuloEspecie(ficha.edicao) },
      { valor: 'ANTECEDENTE', rotulo: 'Antecedente' },
      { valor: 'TALENTO', rotulo: 'Talento' },
      { valor: 'OUTRA', rotulo: 'Outra' },
    ];
  });

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(NOME_MAXIMO)]],
    tipo: new FormControl<TipoHabilidade | ''>('', { nonNullable: true, validators: [Validators.required] }),
    descricao: ['', [Validators.maxLength(DESCRICAO_MAXIMA)]],
  });

  protected readonly tituloDe = (h: Habilidade) => h.nome;

  protected rotuloDoTipo(tipo: TipoHabilidade): string {
    return this.tipos().find((t) => t.valor === tipo)?.rotulo ?? tipo;
  }

  protected itens(): readonly Habilidade[] {
    return this.ficha().habilidades;
  }

  protected preencher(item: Habilidade | undefined): void {
    this.form.reset({ nome: item?.nome ?? '', tipo: item?.tipo ?? '', descricao: item?.descricao ?? '' });
  }

  protected gravar(destino: number | 'novo'): Observable<Habilidade> {
    const { nome, tipo, descricao } = this.form.getRawValue();
    const entrada: HabilidadeEntrada = {
      nome: nome.trim(),
      tipo: tipo as TipoHabilidade,
      descricao: descricao.trim() === '' ? null : descricao,
    };
    const fichaId = this.ficha().id;
    return destino === 'novo' ? this.api.criarHabilidade(fichaId, entrada) : this.api.atualizarHabilidade(fichaId, destino, entrada);
  }

  protected excluir(item: Habilidade): Observable<void> {
    return this.api.excluirHabilidade(this.ficha().id, item.id);
  }

  protected emitir(lista: Habilidade[]): void {
    this.atualizada.emit(lista);
  }
}
