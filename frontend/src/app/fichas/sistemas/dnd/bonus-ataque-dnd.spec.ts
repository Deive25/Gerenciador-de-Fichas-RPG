import { AtaqueDnd } from '../../ficha.model';
import { bonusDoAtaqueDnd } from './regras-dnd';

const ATRIBUTOS = { FOR: 16, DES: 14, CON: 12, INT: 8, SAB: 9, CAR: 10 };

function ataque(parcial: Partial<AtaqueDnd>): AtaqueDnd {
  return { id: 1, nome: 'Espada', atributo: 'FOR', proficiente: false, bonus: 0, dano: '1d8', tipoDano: null, alcance: null, ...parcial };
}

describe('bonusDoAtaqueDnd', () => {
  it('soma o modificador do atributo: FOR 16 = +3', () => {
    expect(bonusDoAtaqueDnd(ataque({}), ATRIBUTOS, 1)).toBe(3);
  });

  it('proficiente soma a proficiencia do nivel (nivel 5 = +3)', () => {
    expect(bonusDoAtaqueDnd(ataque({ proficiente: true }), ATRIBUTOS, 5)).toBe(6);
    expect(bonusDoAtaqueDnd(ataque({ proficiente: false }), ATRIBUTOS, 5)).toBe(3);
  });

  it('soma o bonus do ataque, positivo ou negativo', () => {
    expect(bonusDoAtaqueDnd(ataque({ proficiente: true, bonus: 1 }), ATRIBUTOS, 5)).toBe(7);
    expect(bonusDoAtaqueDnd(ataque({ bonus: -2 }), ATRIBUTOS, 1)).toBe(1);
  });

  it('atributo baixo da modificador negativo: INT 8 = -1 (floor, nao trunc)', () => {
    expect(bonusDoAtaqueDnd(ataque({ atributo: 'INT' }), ATRIBUTOS, 1)).toBe(-1);
    expect(bonusDoAtaqueDnd(ataque({ atributo: 'SAB' }), ATRIBUTOS, 1)).toBe(-1);
  });

  it('usa os valores atuais da ficha, nao os de quando o ataque foi criado', () => {
    const a = ataque({ atributo: 'DES' });
    expect(bonusDoAtaqueDnd(a, ATRIBUTOS, 1)).toBe(2);
    expect(bonusDoAtaqueDnd(a, { ...ATRIBUTOS, DES: 20 }, 1)).toBe(5);
  });
});
