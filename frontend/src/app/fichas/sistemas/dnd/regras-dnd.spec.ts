import { ItemDnd, MagiaDnd } from '../../ficha.model';
import {
  bonusPericia,
  bonusSalvaguarda,
  ataqueMagico,
  capacidadeDeCarga,
  cdDeMagia,
  comSinal,
  formatarPeso,
  modificador,
  ordenarMagias,
  pesoTotal,
  proficiencia,
  rotuloNivelMagia,
} from './regras-dnd';

describe('regras de D&D', () => {
  it('modificador arredonda para baixo, inclusive nos negativos', () => {
    expect(modificador(1)).toBe(-5);
    expect(modificador(8)).toBe(-1);
    expect(modificador(9)).toBe(-1);
    expect(modificador(10)).toBe(0);
    expect(modificador(11)).toBe(0);
    expect(modificador(12)).toBe(1);
    expect(modificador(15)).toBe(2);
    expect(modificador(20)).toBe(5);
    expect(modificador(30)).toBe(10);
  });

  it('proficiencia sobe a cada 4 niveis', () => {
    expect(proficiencia(1)).toBe(2);
    expect(proficiencia(4)).toBe(2);
    expect(proficiencia(5)).toBe(3);
    expect(proficiencia(8)).toBe(3);
    expect(proficiencia(9)).toBe(4);
    expect(proficiencia(13)).toBe(5);
    expect(proficiencia(17)).toBe(6);
    expect(proficiencia(20)).toBe(6);
  });

  it('bonus de pericia: sem grau, proficiente e especialista', () => {
    expect(bonusPericia(14, 5)).toBe(2);
    expect(bonusPericia(14, 5, undefined)).toBe(2);
    expect(bonusPericia(14, 5, 'PROFICIENTE')).toBe(5);
    expect(bonusPericia(14, 5, 'ESPECIALISTA')).toBe(8);
    expect(bonusPericia(9, 1, 'PROFICIENTE')).toBe(1);
    expect(bonusPericia(9, 1)).toBe(-1);
  });

  it('bonus de salvaguarda soma a proficiencia so se proficiente', () => {
    expect(bonusSalvaguarda(14, 5, false)).toBe(2);
    expect(bonusSalvaguarda(14, 5, true)).toBe(5);
    expect(bonusSalvaguarda(7, 1, true)).toBe(0);
  });

  it('comSinal usa o sinal de menos (U+2212) e +0 para zero', () => {
    expect(comSinal(2)).toBe('+2');
    expect(comSinal(0)).toBe('+0');
    expect(comSinal(-1)).toBe('−1');
    expect(comSinal(-1)).not.toBe('-1');
  });
});

describe('carga de D&D', () => {
  const item = (peso: number, quantidade: number): ItemDnd => ({
    id: 1, nome: 'Item', quantidade, peso, equipado: false, descricao: null,
  });

  it('a capacidade e FOR x 7,5 kg', () => {
    expect(capacidadeDeCarga(1)).toBe(7.5);
    expect(capacidadeDeCarga(10)).toBe(75);
    expect(capacidadeDeCarga(15)).toBe(112.5);
  });

  it('o peso total soma peso x quantidade sem ruido de ponto flutuante', () => {
    expect(pesoTotal([])).toBe(0);
    expect(pesoTotal([item(0.1, 3)])).toBe(0.3);
    expect(pesoTotal([item(1.5, 2), item(4, 1)])).toBe(7);
  });

  it('formata o peso em pt-BR, com ate 2 casas e sem zeros a direita', () => {
    expect(formatarPeso(112.5)).toBe('112,5');
    expect(formatarPeso(7)).toBe('7');
    expect(formatarPeso(0.25)).toBe('0,25');
  });
});

describe('magias de D&D', () => {
  // Atributo 16 = modificador +3.
  it.each([
    [1, 13, 5],
    [5, 14, 6],
    [17, 17, 9],
  ])('nivel %i com atributo 16: CD %i e ataque +%i', (nivel, cd, ataque) => {
    expect(cdDeMagia(nivel, 16)).toBe(cd);
    expect(ataqueMagico(nivel, 16)).toBe(ataque);
  });

  it('sem atributo de conjuracao nao ha CD nem ataque', () => {
    expect(cdDeMagia(5, null)).toBeNull();
    expect(ataqueMagico(5, null)).toBeNull();
  });

  it('modificador negativo reduz a CD e o ataque', () => {
    expect(cdDeMagia(1, 8)).toBe(9);
    expect(ataqueMagico(1, 8)).toBe(1);
  });

  it('rotuloNivelMagia: 0 e Truque, os outros n-esimo nivel', () => {
    expect(rotuloNivelMagia(0)).toBe('Truque');
    expect(rotuloNivelMagia(1)).toBe('1º nível');
    expect(rotuloNivelMagia(9)).toBe('9º nível');
  });

  it('ordenarMagias ordena por nivel e depois por id, sem mudar a lista original', () => {
    const m = (id: number, nivel: number): MagiaDnd => ({
      id, nome: `M${id}`, nivel, escola: null, tempo: null, alcance: null, componentes: null, duracao: null,
      concentracao: false, ritual: false, preparada: false, descricao: null,
    });
    const original = [m(5, 3), m(2, 3), m(9, 0), m(1, 1)];
    expect(ordenarMagias(original).map((x) => x.id)).toEqual([9, 1, 2, 5]);
    expect(original.map((x) => x.id)).toEqual([5, 2, 9, 1]);
  });
});
