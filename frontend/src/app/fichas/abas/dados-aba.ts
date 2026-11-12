import { Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Contagem, contagemZerada, escreverNotacao, lerNotacao } from '../dado/notacao-de-dados';
import { LADOS_DOS_DADOS, LadosDoDado, PedidoDeRolagem } from '../ficha.model';
import { mensagemDoControle, notacao } from '../sistemas/comum/formulario-comum';

const MAXIMO_POR_DADO = 20;
const MAXIMO_NO_TOTAL = 20;
const MAXIMO_DO_MODIFICADOR = 99;

/**
 * Aba Dados (os dois sistemas): rolagem livre por botões e por texto. O campo guarda a notação ("2d6+1d4+3")
 * e os contadores (um por dado, mais o modificador) mostram o mesmo: os botões reescrevem o texto na forma
 * canônica e um texto válido digitado atualiza os contadores. O erro de um texto inválido aparece abaixo do
 * campo. A aba só emite o pedido (`rolar`): quem chama a API e mostra o resultado é a tela da ficha.
 */
@Component({
  selector: 'app-dados-aba',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule],
  templateUrl: './dados-aba.html',
  styleUrl: './dados-aba.scss',
})
export class DadosAba {
  /** Há uma rolagem em andamento: o "Rolar" fica desabilitado. */
  readonly rolando = input(false);
  readonly rolar = output<PedidoDeRolagem>();

  protected readonly lados = LADOS_DOS_DADOS;
  protected readonly maximoPorDado = MAXIMO_POR_DADO;
  protected readonly maximoDoModificador = MAXIMO_DO_MODIFICADOR;

  /** O campo de texto; o FormControl existe para o Material mostrar o erro abaixo dele (`notacao`). */
  protected readonly campo = new FormControl('', { nonNullable: true, validators: [notacao()] });
  protected readonly mensagem = mensagemDoControle;
  /** O mesmo texto do campo, como signal, para as contas abaixo. */
  protected readonly texto = signal('');
  protected readonly contagem = signal<Contagem>(contagemZerada());
  protected readonly modificador = signal(0);

  /** Dados somados de todos os tipos: a gramática aceita no máximo 20. */
  protected readonly totalDeDados = computed(() => LADOS_DOS_DADOS.reduce((soma, l) => soma + this.contagem()[l], 0));
  protected readonly maximoNoTotal = MAXIMO_NO_TOTAL;

  private readonly leitura = computed(() => lerNotacao(this.texto()));
  protected readonly podeRolar = computed(() => this.leitura().ok && !this.rolando());

  protected readonly modificadorEscrito = computed(() => {
    const m = this.modificador();
    return m < 0 ? `−${-m}` : m > 0 ? `+${m}` : '0';
  });

  constructor() {
    const assinatura = this.campo.valueChanges.subscribe((texto) => this.aoDigitar(texto));
    inject(DestroyRef).onDestroy(() => assinatura.unsubscribe());
  }

  /** A pessoa digitou: texto válido atualiza os contadores, vazio os zera, inválido só mostra o erro. */
  private aoDigitar(texto: string): void {
    this.campo.markAsTouched(); // o Material só pinta o erro em campo tocado, e aqui o erro aparece enquanto se digita
    this.texto.set(texto);
    const leitura = lerNotacao(texto);
    if (leitura.ok) {
      this.contagem.set(leitura.contagem);
      this.modificador.set(leitura.modificador);
    } else if (texto.trim() === '') {
      this.zerar();
    }
  }

  protected mudarDado(lados: LadosDoDado, passo: 1 | -1): void {
    const atual = this.contagem()[lados];
    const novo = Math.min(MAXIMO_POR_DADO, Math.max(0, atual + passo));
    if (novo === atual || (passo > 0 && this.totalDeDados() >= MAXIMO_NO_TOTAL)) {
      return;
    }
    this.contagem.update((c) => ({ ...c, [lados]: novo }));
    this.reescrever();
  }

  protected mudarModificador(passo: 1 | -1): void {
    this.modificador.update((m) => Math.min(MAXIMO_DO_MODIFICADOR, Math.max(-MAXIMO_DO_MODIFICADOR, m + passo)));
    this.reescrever();
  }

  protected limpar(): void {
    this.zerar();
    this.escrever('');
    this.campo.markAsPristine();
  }

  protected pedir(): void {
    const leitura = this.leitura();
    if (!leitura.ok || this.rolando()) {
      return;
    }
    this.rolar.emit({ tipo: 'livre', expressao: escreverNotacao(leitura.contagem, leitura.modificador) });
  }

  private zerar(): void {
    this.contagem.set(contagemZerada());
    this.modificador.set(0);
  }

  private reescrever(): void {
    this.escrever(escreverNotacao(this.contagem(), this.modificador()));
  }

  /** Os botões escrevem no campo sem passar por `aoDigitar` (os contadores já estão certos). */
  private escrever(texto: string): void {
    this.texto.set(texto);
    this.campo.setValue(texto, { emitEvent: false });
  }
}
