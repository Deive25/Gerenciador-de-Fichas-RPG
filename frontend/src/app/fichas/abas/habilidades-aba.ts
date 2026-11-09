import { ChangeDetectorRef, Component, ElementRef, Injector, computed, inject, input, output, signal } from '@angular/core';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { ListaEditavel } from '../componentes/lista-editavel/lista-editavel';
import { FichaApi } from '../ficha-api';
import { Ficha, Habilidade, HabilidadeEntrada, TipoHabilidade } from '../ficha.model';
import {
  aplicarErrosDoServidor,
  focarPrimeiroInvalido,
  focarPrimeiroInvalidoDepois,
  mensagemDoControle,
} from '../sistemas/comum/formulario-comum';
import { rotuloEspecie } from '../sistemas/dnd/catalogo-dnd';

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
 * fala com a API e mantém o formulário. A cada gravação ela avisa a lista nova (`atualizada`) e o
 * detalhe troca `ficha.habilidades` sem recarregar a ficha.
 */
@Component({
  selector: 'app-habilidades-aba',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, ListaEditavel],
  templateUrl: './habilidades-aba.html',
  styleUrl: './habilidades-aba.scss',
})
export class HabilidadesAba {
  private readonly api = inject(FichaApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly ficha = input.required<Ficha>();
  /** A lista de habilidades depois de uma gravação; o detalhe a coloca na ficha. */
  readonly atualizada = output<Habilidade[]>();

  protected readonly editando = signal<number | 'novo' | null>(null);
  protected readonly salvando = signal(false);
  /** Erro que não é de um campo (ex.: servidor indisponível, falha ao remover). */
  protected readonly erroGeral = signal<ApiError | null>(null);

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

  /** Abrir ou trocar o formulário: o formulário começa limpo (novo) ou com os valores do item. */
  protected aoMudarEdicao(destino: number | 'novo' | null): void {
    this.editando.set(destino);
    this.erroGeral.set(null);
    const item = typeof destino === 'number' ? this.ficha().habilidades.find((h) => h.id === destino) : undefined;
    this.form.reset({ nome: item?.nome ?? '', tipo: item?.tipo ?? '', descricao: item?.descricao ?? '' });
  }

  protected cancelar(): void {
    this.aoMudarEdicao(null);
  }

  protected readonly mensagem = mensagemDoControle;

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
    const { nome, tipo, descricao } = this.form.getRawValue();
    const entrada: HabilidadeEntrada = {
      nome: nome.trim(),
      tipo: tipo as TipoHabilidade,
      descricao: descricao.trim() === '' ? null : descricao,
    };
    const fichaId = this.ficha().id;
    const requisicao: Observable<Habilidade> =
      destino === 'novo' ? this.api.criarHabilidade(fichaId, entrada) : this.api.atualizarHabilidade(fichaId, destino, entrada);

    this.erroGeral.set(null);
    this.salvando.set(true);
    requisicao.subscribe({
      next: (gravada) => {
        this.salvando.set(false);
        const atual = this.ficha().habilidades;
        this.atualizada.emit(destino === 'novo' ? [...atual, gravada] : atual.map((h) => (h.id === gravada.id ? gravada : h)));
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

  protected remover(item: Habilidade): void {
    if (this.salvando()) {
      return;
    }
    this.erroGeral.set(null);
    this.salvando.set(true);
    this.api.excluirHabilidade(this.ficha().id, item.id).subscribe({
      next: () => {
        this.salvando.set(false);
        this.atualizada.emit(this.ficha().habilidades.filter((h) => h.id !== item.id));
      },
      error: (erro: ApiError) => {
        this.salvando.set(false);
        this.erroGeral.set(erro);
      },
    });
  }
}
