import { TestBed } from '@angular/core/testing';
import { TestePericiaResultado } from '../ficha.model';
import { ResultadoRolagem } from './resultado-rolagem';

function montar(resultado: TestePericiaResultado) {
  const fixture = TestBed.createComponent(ResultadoRolagem);
  fixture.componentRef.setInput('resultado', resultado);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('ResultadoRolagem', () => {
  it('mostra o resultado e as faixas', () => {
    const el = montar({ pericia: 'INVESTIGACAO', nome: 'Investigação', valor: 70, rolagem: 23, resultado: 'SUCESSO' });
    expect(el.textContent).toContain('Investigação: 23 → Sucesso');
    expect(el.textContent).toContain('extremo ≤ 14 · sucesso ≤ 70');
    expect(el.querySelector('.sucesso')).not.toBeNull();
  });

  it('explica a pericia sem treino', () => {
    const el = montar({ pericia: 'CHARME', nome: 'Charme', valor: 0, rolagem: 42, resultado: 'FALHA' });
    expect(el.textContent).toContain('Charme: 42 → Falha');
    expect(el.textContent).toContain('sem treino: sempre falha');
    expect(el.querySelector('.falha')).not.toBeNull();
  });
});
