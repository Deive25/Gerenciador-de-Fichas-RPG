import { Component, computed, input } from '@angular/core';
import { faixas, rotuloResultado } from '../catalogo';
import { TestePericiaResultado } from '../ficha.model';

/** Resultado de um teste de perícia, com as faixas que explicam o porquê. */
@Component({
  selector: 'app-resultado-rolagem',
  templateUrl: './resultado-rolagem.html',
  styleUrl: './resultado-rolagem.scss',
})
export class ResultadoRolagem {
  readonly resultado = input.required<TestePericiaResultado>();

  readonly classe = computed(() => {
    const r = this.resultado().resultado;
    return r === 'SUCESSO_EXTREMO' ? 'extremo' : r === 'SUCESSO' ? 'sucesso' : 'falha';
  });

  readonly titulo = computed(() => {
    const r = this.resultado();
    return `${r.nome}: ${r.rolagem} → ${rotuloResultado(r.resultado)}`;
  });

  readonly explicacao = computed(() => {
    const valor = this.resultado().valor;
    if (valor <= 0) {
      return 'sem treino: sempre falha';
    }
    const f = faixas(valor);
    return `extremo ≤ ${f.extremo} · sucesso ≤ ${f.sucesso}`;
  });
}
