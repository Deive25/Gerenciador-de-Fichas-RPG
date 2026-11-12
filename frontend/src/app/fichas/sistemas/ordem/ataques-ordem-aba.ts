import { Component, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';
import { AbaDeLista } from '../../abas/aba-de-lista';
import { ListaEditavel } from '../../componentes/lista-editavel/lista-editavel';
import { FichaApi } from '../../ficha-api';
import { AtaqueOrdem, AtaqueOrdemEntrada, FichaOrdem, PedidoDeRolagem } from '../../ficha.model';
import { INTEIRO, faixa, notacao, paraNumero, textoDe, textoOuNulo } from '../comum/formulario-comum';
import { comSinal } from '../dnd/regras-dnd';
import { PERICIAS_ORDEM, bonusDoAtaqueOrdem } from './catalogo-ordem';

const NOME_MAXIMO = 255;
const TEXTO_MAXIMO = 255;

/** As duas perícias de ataque, com o nome do catálogo. */
const PERICIAS_DE_ATAQUE = PERICIAS_ORDEM.filter((p) => p.codigo === 'LUTA' || p.codigo === 'PONTARIA');

/**
 * Aba Ataques de Ordem: a lista de ataques, cada um com o bônus (grau da perícia + bônus do ataque), o dano
 * e a margem de crítico. O salvar e o remover vêm da AbaDeLista; o bônus é função pura do catálogo. O
 * "Rolar" só emite o pedido (`rolar`): quem chama a API e mostra o resultado é a tela da ficha.
 */
@Component({
  selector: 'app-ataques-ordem-aba',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, ListaEditavel],
  templateUrl: './ataques-ordem-aba.html',
  styleUrl: './ataques-ordem-aba.scss',
})
export class AtaquesOrdemAba extends AbaDeLista<AtaqueOrdem> {
  private readonly api = inject(FichaApi);

  readonly ficha = input.required<FichaOrdem>();
  /** Há uma rolagem em andamento: todos os "Rolar" ficam desabilitados. */
  readonly rolando = input(false);
  /** A lista de ataques depois de uma gravação; o detalhe a coloca na ficha. */
  readonly atualizada = output<AtaqueOrdem[]>();
  readonly rolar = output<PedidoDeRolagem>();

  protected readonly pericias = PERICIAS_DE_ATAQUE;

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(NOME_MAXIMO)]],
    pericia: ['LUTA' as AtaqueOrdem['pericia'], [Validators.required]],
    bonus: ['0', [Validators.required, Validators.pattern(INTEIRO), faixa(-99, 99)]],
    dano: ['', [Validators.required, notacao()]],
    tipoDano: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    alcance: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    margem: ['20', [Validators.required, Validators.pattern(INTEIRO), faixa(2, 20)]],
    multiplicador: ['2', [Validators.required, Validators.pattern(INTEIRO), faixa(2, 5)]],
  });

  protected readonly tituloDe = (a: AtaqueOrdem) => a.nome;

  /** "Luta · +5 · 1d8+3 cortante · crítico 19/×3": o bônus vem dos valores atuais da ficha. */
  protected resumo(a: AtaqueOrdem): string {
    const pericia = PERICIAS_ORDEM.find((p) => p.codigo === a.pericia)?.nome ?? a.pericia;
    const bonus = bonusDoAtaqueOrdem(a, this.ficha().pericias);
    const dano = a.tipoDano ? `${a.dano} ${a.tipoDano}` : a.dano;
    return `${pericia} · ${comSinal(bonus)} · ${dano} · crítico ${a.margem}/×${a.multiplicador}`;
  }

  protected pedir(a: AtaqueOrdem): void {
    this.rolar.emit({ tipo: 'ataque', ataqueId: a.id });
  }

  protected itens(): readonly AtaqueOrdem[] {
    return this.ficha().ataques;
  }

  protected preencher(ataque: AtaqueOrdem | undefined): void {
    this.form.reset({
      nome: ataque?.nome ?? '',
      pericia: ataque?.pericia ?? 'LUTA',
      bonus: textoDe(ataque?.bonus ?? 0),
      dano: ataque?.dano ?? '',
      tipoDano: ataque?.tipoDano ?? '',
      alcance: ataque?.alcance ?? '',
      margem: textoDe(ataque?.margem ?? 20),
      multiplicador: textoDe(ataque?.multiplicador ?? 2),
    });
  }

  protected gravar(destino: number | 'novo'): Observable<AtaqueOrdem> {
    const v = this.form.getRawValue();
    // Os números só viram número aqui: os validadores já aprovaram o texto.
    const entrada: AtaqueOrdemEntrada = {
      nome: v.nome.trim(),
      pericia: v.pericia,
      bonus: paraNumero(v.bonus) as number,
      dano: v.dano.trim(),
      tipoDano: textoOuNulo(v.tipoDano),
      alcance: textoOuNulo(v.alcance),
      margem: paraNumero(v.margem) as number,
      multiplicador: paraNumero(v.multiplicador) as number,
    };
    const fichaId = this.ficha().id;
    return destino === 'novo'
      ? this.api.criarAtaque<AtaqueOrdem>(fichaId, entrada)
      : this.api.atualizarAtaque<AtaqueOrdem>(fichaId, destino, entrada);
  }

  protected excluir(ataque: AtaqueOrdem): Observable<void> {
    return this.api.excluirAtaque(this.ficha().id, ataque.id);
  }

  protected emitir(lista: AtaqueOrdem[]): void {
    this.atualizada.emit(lista);
  }
}
