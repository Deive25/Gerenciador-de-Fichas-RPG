import { Component, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';
import { AbaDeLista } from '../../abas/aba-de-lista';
import { ListaEditavel } from '../../componentes/lista-editavel/lista-editavel';
import { SeletorDeModo } from '../../componentes/seletor-de-modo';
import { FichaApi } from '../../ficha-api';
import { AtaqueDnd, AtaqueDndEntrada, AtributoDnd, FichaDnd, ModoTeste, PedidoDeRolagem } from '../../ficha.model';
import { INTEIRO, faixa, notacao, paraNumero, textoDe, textoOuNulo } from '../comum/formulario-comum';
import { ATRIBUTOS_DND } from './catalogo-dnd';
import { bonusDoAtaqueDnd, comSinal } from './regras-dnd';

const NOME_MAXIMO = 255;
const TEXTO_MAXIMO = 255;

/**
 * Aba Ataques de D&D: o seletor Normal · Vantagem · Desvantagem e a lista de ataques, cada um com o bônus
 * (modificador + proficiência, se proficiente, + bônus do ataque) e o dano. O salvar e o remover vêm da
 * AbaDeLista; o bônus é função pura de regras-dnd. O "Rolar" só emite o pedido (`rolar`), com o modo
 * escolhido, e o seletor volta a Normal: quem chama a API e mostra o resultado é a tela da ficha.
 */
@Component({
  selector: 'app-ataques-dnd-aba',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    ListaEditavel,
    SeletorDeModo,
  ],
  templateUrl: './ataques-dnd-aba.html',
  styleUrl: './ataques-dnd-aba.scss',
})
export class AtaquesDndAba extends AbaDeLista<AtaqueDnd> {
  private readonly api = inject(FichaApi);

  readonly ficha = input.required<FichaDnd>();
  /** Há uma rolagem em andamento: todos os "Rolar" ficam desabilitados. */
  readonly rolando = input(false);
  /** A lista de ataques depois de uma gravação; o detalhe a coloca na ficha. */
  readonly atualizada = output<AtaqueDnd[]>();
  readonly rolar = output<PedidoDeRolagem>();

  protected readonly modo = signal<ModoTeste>('NORMAL');
  protected readonly atributos = ATRIBUTOS_DND;

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(NOME_MAXIMO)]],
    atributo: ['FOR' as AtributoDnd, [Validators.required]],
    proficiente: [false],
    bonus: ['0', [Validators.required, Validators.pattern(INTEIRO), faixa(-99, 99)]],
    dano: ['', [Validators.required, notacao()]],
    tipoDano: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
    alcance: ['', [Validators.maxLength(TEXTO_MAXIMO)]],
  });

  protected readonly tituloDe = (a: AtaqueDnd) => a.nome;

  /** "+5 · 1d8+3 cortante": o bônus vem dos valores atuais da ficha. */
  protected resumo(a: AtaqueDnd): string {
    const f = this.ficha();
    const dano = a.tipoDano ? `${a.dano} ${a.tipoDano}` : a.dano;
    return `${comSinal(bonusDoAtaqueDnd(a, f.atributos, f.nivel))} · ${dano}`;
  }

  /** Pede a rolagem com o modo escolhido (sem `modo` no Normal) e volta o seletor a Normal. */
  protected pedir(a: AtaqueDnd): void {
    const modo = this.modo();
    this.rolar.emit(modo === 'NORMAL' ? { tipo: 'ataque', ataqueId: a.id } : { tipo: 'ataque', ataqueId: a.id, modo });
    this.modo.set('NORMAL');
  }

  protected itens(): readonly AtaqueDnd[] {
    return this.ficha().ataques;
  }

  protected preencher(ataque: AtaqueDnd | undefined): void {
    this.form.reset({
      nome: ataque?.nome ?? '',
      atributo: ataque?.atributo ?? 'FOR',
      proficiente: ataque?.proficiente ?? false,
      bonus: textoDe(ataque?.bonus ?? 0),
      dano: ataque?.dano ?? '',
      tipoDano: ataque?.tipoDano ?? '',
      alcance: ataque?.alcance ?? '',
    });
  }

  protected gravar(destino: number | 'novo'): Observable<AtaqueDnd> {
    const v = this.form.getRawValue();
    const entrada: AtaqueDndEntrada = {
      nome: v.nome.trim(),
      atributo: v.atributo,
      proficiente: v.proficiente,
      bonus: paraNumero(v.bonus) as number,
      dano: v.dano.trim(),
      tipoDano: textoOuNulo(v.tipoDano),
      alcance: textoOuNulo(v.alcance),
    };
    const fichaId = this.ficha().id;
    return destino === 'novo'
      ? this.api.criarAtaque<AtaqueDnd>(fichaId, entrada)
      : this.api.atualizarAtaque<AtaqueDnd>(fichaId, destino, entrada);
  }

  protected excluir(ataque: AtaqueDnd): Observable<void> {
    return this.api.excluirAtaque(this.ficha().id, ataque.id);
  }

  protected emitir(lista: AtaqueDnd[]): void {
    this.atualizada.emit(lista);
  }
}
