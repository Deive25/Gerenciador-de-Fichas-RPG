import { Contagem, contagemZerada, escreverNotacao, lerNotacao } from './notacao-de-dados';

/** Só os dados que aparecem, para o teste ficar legível. */
function so(contagem: Contagem): Record<string, number> {
  return Object.fromEntries(Object.entries(contagem).filter(([, n]) => n > 0));
}

function lida(texto: string) {
  const r = lerNotacao(texto);
  if (!r.ok) {
    throw new Error(`esperava válido: ${texto} -> ${r.erro}`);
  }
  return { dados: so(r.contagem), modificador: r.modificador };
}

function erroDe(texto: string): string {
  const r = lerNotacao(texto);
  if (r.ok) {
    throw new Error(`esperava inválido: ${texto}`);
  }
  return r.erro;
}

describe('lerNotacao · validos (os mesmos do servidor)', () => {
  it.each([
    ['1d8+3', { 8: 1 }, 3],
    ['2d6+1d4+2', { 4: 1, 6: 2 }, 2],
    ['1d10-1', { 10: 1 }, -1],
    ['2d6+1d4+3', { 4: 1, 6: 2 }, 3],
    ['1d20', { 20: 1 }, 0],
    ['20d6', { 6: 20 }, 0],
    ['1d100', { 100: 1 }, 0],
    ['1d4+1d6+1d8+1d10+1d12+1d20+1d100', { 4: 1, 6: 1, 8: 1, 10: 1, 12: 1, 20: 1, 100: 1 }, 0],
    ['5', {}, 5],
    ['0', {}, 0],
    ['99', {}, 99],
  ])('%s', (texto, dados, modificador) => {
    expect(lida(texto)).toEqual({ dados, modificador });
  });

  it('espacos sao ignorados, tambem no meio do termo', () => {
    expect(lida(' 2d6 + 1d4 + 3 ')).toEqual({ dados: { 4: 1, 6: 2 }, modificador: 3 });
    expect(lida('2 d 6')).toEqual({ dados: { 6: 2 }, modificador: 0 });
  });

  it('o D maiusculo vale', () => {
    expect(lida('2D6+1D4')).toEqual({ dados: { 4: 1, 6: 2 }, modificador: 0 });
  });

  it('o mesmo dado em dois termos soma: 1d6+1d6 e d6: 2', () => {
    expect(lida('1d6+1d6')).toEqual({ dados: { 6: 2 }, modificador: 0 });
  });

  it('constantes somam com o sinal de cada uma: 1d4+5-2 tem modificador 3', () => {
    expect(lida('1d4+5-2')).toEqual({ dados: { 4: 1 }, modificador: 3 });
    expect(lida('1d4+2+3')).toEqual({ dados: { 4: 1 }, modificador: 5 });
  });

  it('a constante pode vir antes dos dados', () => {
    expect(lida('3+1d6')).toEqual({ dados: { 6: 1 }, modificador: 3 });
  });

  it('8 termos e 20 dados somados ainda valem', () => {
    expect(lida('1d4+1d6+1d8+1d10+1d12+1d20+1d100+3')).toEqual({
      dados: { 4: 1, 6: 1, 8: 1, 10: 1, 12: 1, 20: 1, 100: 1 }, modificador: 3,
    });
    expect(lida('10d6+10d4')).toEqual({ dados: { 4: 10, 6: 10 }, modificador: 0 });
  });
});

describe('lerNotacao · invalidos', () => {
  it.each(['d6', '0d6', '1d7', '21d6', 'abc', '', '   ', '1d', 'd', '2d6x', '1.5d6', '1d6;1d4', '1d3', '1d0', '1d6d6'])(
    '"%s" nao vale',
    (texto) => {
      expect(lerNotacao(texto).ok).toBe(false);
    },
  );

  it('9 termos nao valem (o maximo e 8)', () => {
    expect(lerNotacao('1+1+1+1+1+1+1+1+1').ok).toBe(false);
    expect(lerNotacao('1+1+1+1+1+1+1+1').ok).toBe(true); // 8 termos
  });

  it('21 dados somados nao valem, nem em termos separados', () => {
    expect(lerNotacao('20d6+1d4').ok).toBe(false);
    expect(lerNotacao('11d6+10d4').ok).toBe(false);
    expect(lerNotacao('1d6+1d6+19d4').ok).toBe(false);
  });

  it('dado depois de - nao vale, mas constante vale', () => {
    expect(lerNotacao('1d8-1d4').ok).toBe(false);
    expect(lerNotacao('1d8-2').ok).toBe(true);
  });

  it('constante maior que 99 nao vale', () => {
    expect(lerNotacao('100').ok).toBe(false);
    expect(lerNotacao('1d6+100').ok).toBe(false);
  });

  it('sinal sobrando ou faltando nao vale', () => {
    expect(lerNotacao('+1d6').ok).toBe(false);
    expect(lerNotacao('-3').ok).toBe(false);
    expect(lerNotacao('1d6+').ok).toBe(false);
    expect(lerNotacao('1d6++2').ok).toBe(false);
    expect(lerNotacao('1d6+-2').ok).toBe(false);
  });

  it('as mensagens dizem o que esta errado', () => {
    expect(erroDe('')).toBe('Digite os dados, por exemplo 2d6+3.');
    expect(erroDe('abc')).toBe('Não entendi “abc”. Use termos como 2d6 ou 3, ligados por + ou -.');
    expect(erroDe('d6')).toBe('Escreva quantos dados antes do d, por exemplo 1d6.');
    expect(erroDe('0d6')).toBe('Use de 1 a 20 dados em cada termo.');
    expect(erroDe('21d6')).toBe('Use de 1 a 20 dados em cada termo.');
    expect(erroDe('1d7')).toBe('Não existe d7. Use d4, d6, d8, d10, d12, d20 ou d100.');
    expect(erroDe('100')).toBe('Use números de 0 a 99.');
    expect(erroDe('1+1+1+1+1+1+1+1+1')).toBe('Use no máximo 8 termos.');
    expect(erroDe('20d6+1d4')).toBe('Use no máximo 20 dados no total.');
    expect(erroDe('1d8-1d4')).toBe('Dados não podem ser subtraídos; só números.');
    expect(erroDe('1d6+')).toBe('Falta um termo ao lado do sinal.');
    expect(erroDe('+1d6')).toBe('Comece pelo dado ou número, sem sinal.');
  });
});

describe('escreverNotacao · forma canonica', () => {
  const com = (parcial: Record<number, number>): Contagem => ({ ...contagemZerada(), ...parcial });

  it('dados de menos lados primeiro, depois o modificador', () => {
    expect(escreverNotacao(com({ 6: 2, 4: 1 }), 3)).toBe('1d4+2d6+3');
  });

  it('sem modificador, sem sinal sobrando', () => {
    expect(escreverNotacao(com({ 20: 1 }), 0)).toBe('1d20');
  });

  it('modificador negativo vira - e positivo vira +', () => {
    expect(escreverNotacao(com({ 10: 1 }), -1)).toBe('1d10-1');
    expect(escreverNotacao(com({ 12: 1 }), 4)).toBe('1d12+4');
  });

  it('so o modificador positivo vira o proprio numero; sem dado e sem modificador positivo fica vazio', () => {
    expect(escreverNotacao(contagemZerada(), 3)).toBe('3');
    expect(escreverNotacao(contagemZerada(), 0)).toBe('');
    expect(escreverNotacao(contagemZerada(), -2)).toBe('');
  });

  it('os sete dados e o modificador somam 8 termos, o maximo da gramatica', () => {
    const tudo = com({ 4: 1, 6: 1, 8: 1, 10: 1, 12: 1, 20: 1, 100: 1 });
    expect(escreverNotacao(tudo, 1)).toBe('1d4+1d6+1d8+1d10+1d12+1d20+1d100+1');
  });

  it('o que se escreve se le de volta igual', () => {
    const contagem = com({ 4: 3, 8: 2, 100: 1 });
    const r = lerNotacao(escreverNotacao(contagem, -5));
    expect(r.ok && r.contagem).toEqual(contagem);
    expect(r.ok && r.modificador).toBe(-5);
  });
});
