import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Primeiro passo de "nova ficha": escolher o sistema; cada cartão leva ao formulário do seu sistema. */
@Component({
  selector: 'app-escolha-sistema',
  imports: [RouterLink],
  templateUrl: './escolha-sistema.html',
  styleUrl: './escolha-sistema.scss',
})
export class EscolhaSistema {
  /** Vem do `?campanha=` (withComponentInputBinding): a nova ficha nasce nessa campanha, e o parâmetro segue até o formulário. */
  readonly campanha = input<string | undefined>();
  protected readonly parametros = computed(() => {
    const campanha = this.campanha();
    return campanha ? { campanha } : {};
  });
  protected readonly voltarPara = computed(() => {
    const campanha = this.campanha();
    return campanha ? ['/campanhas', campanha] : ['/fichas'];
  });
}
