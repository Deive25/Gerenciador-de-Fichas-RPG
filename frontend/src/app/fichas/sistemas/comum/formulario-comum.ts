import { ChangeDetectorRef, Injector, afterNextRender } from '@angular/core';
import { AbstractControl, FormControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { ApiError } from '../../../core/api-error';

// Peças que os formulários de todos os sistemas usam do mesmo jeito (2.1/2.1d): validação de
// inteiros, erro do servidor no campo e foco no primeiro campo inválido. Ficam aqui para
// um sistema novo não copiar nada e para uma correção valer para todos.

/** Inteiro com sinal opcional; vazio é permitido (vira null). */
export const INTEIRO = /^-?\d+$/;

/** Inteiro entre `minimo` e `maximo` (sem teto se omitido). Vazio e não numérico ficam para os outros validadores. */
export function faixa(minimo: number, maximo = Infinity): ValidatorFn {
  return (controle: AbstractControl): ValidationErrors | null => {
    const texto = String(controle.value ?? '').trim();
    if (!INTEIRO.test(texto)) {
      return null;
    }
    const numero = Number(texto);
    return numero < minimo || numero > maximo
      ? { faixa: maximo === Infinity ? `${minimo} ou mais` : `De ${minimo} a ${maximo}` }
      : null;
  };
}

/** "atributos[AGI]" (Bean Validation) e "atributos.AGI" (regra) apontam para o mesmo campo: "atributos.AGI". */
export function caminhoDoCampo(chave: string): string {
  return chave.replace(/\[([^\]]+)\]/g, '.$1');
}

/** Foca o primeiro campo marcado como inválido (campo de texto ou lista) dentro do formulário. */
export function focarPrimeiroInvalido(host: HTMLElement): void {
  host.querySelector<HTMLElement>('input.ng-invalid, mat-select.ng-invalid')?.focus();
}

/**
 * Cola cada erro do servidor no controle da chave (nas duas formas de chave), mantendo os erros
 * do próprio formulário. Devolve verdadeiro se algum caiu num campo; falso = só erro geral.
 */
export function aplicarErrosDoServidor(form: AbstractControl, erro: ApiError): boolean {
  let algumNoCampo = false;
  for (const [chave, texto] of Object.entries(erro.campos)) {
    const controle = form.get(caminhoDoCampo(chave));
    if (controle instanceof FormControl) {
      controle.setErrors({ ...controle.errors, servidor: texto });
      controle.markAsTouched();
      algumNoCampo = true;
    }
  }
  return algumNoCampo;
}

/**
 * Leva a pessoa até o primeiro campo inválido depois da próxima renderização (o campo precisa já estar
 * com .ng-invalid no DOM). O erro pode estar longe da tela: as perícias ficam no fim do formulário.
 */
export function focarPrimeiroInvalidoDepois(host: HTMLElement, injector: Injector, cdr: ChangeDetectorRef): void {
  cdr.markForCheck();
  afterNextRender(() => focarPrimeiroInvalido(host), { injector });
}

/** Mensagem do primeiro erro do controle, na ordem: servidor, obrigatório, tamanho, inteiro, faixa. */
export function mensagemDoControle(controle: AbstractControl): string | null {
  const erros = controle.errors;
  if (!erros || !(controle.touched || controle.dirty)) {
    return null;
  }
  if (erros['servidor']) {
    return erros['servidor'] as string;
  }
  if (erros['required']) {
    return 'Obrigatório';
  }
  if (erros['maxlength']) {
    return `Máximo de ${erros['maxlength'].requiredLength} caracteres`;
  }
  if (erros['pattern']) {
    return 'Use um número inteiro';
  }
  if (erros['faixa']) {
    return erros['faixa'] as string;
  }
  return null;
}

/** Monta um objeto com uma entrada por chave (os atributos e as perícias de cada sistema). */
export function registro<K extends string, V>(chaves: readonly K[], fabrica: (chave: K) => V): Record<K, V> {
  return Object.fromEntries(chaves.map((chave) => [chave, fabrica(chave)])) as Record<K, V>;
}

/** Campo de texto vazio vira null; senão, o número. */
export function paraNumero(texto: string): number | null {
  const limpo = texto.trim();
  return limpo === '' ? null : Number(limpo);
}

export function textoDe(numero: number | null): string {
  return numero === null ? '' : String(numero);
}
