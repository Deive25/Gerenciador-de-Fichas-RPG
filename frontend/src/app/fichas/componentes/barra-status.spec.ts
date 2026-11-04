import { TestBed } from '@angular/core/testing';
import { BarraStatus } from './barra-status';

function criar(atual: number | null, maximo: number | null) {
  const fixture = TestBed.createComponent(BarraStatus);
  fixture.componentRef.setInput('rotulo', 'Vida');
  fixture.componentRef.setInput('tipo', 'vida');
  fixture.componentRef.setInput('atual', atual);
  fixture.componentRef.setInput('maximo', maximo);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('BarraStatus', () => {
  it('mostra a proporcao', () => {
    const el = criar(9, 12);
    expect(el.textContent).toContain('9/12');
    expect((el.querySelector('.barra-preenchida') as HTMLElement).style.width).toBe('75%');
  });

  it('limita a barra entre 0 e 100', () => {
    expect((criar(15, 12).querySelector('.barra-preenchida') as HTMLElement).style.width).toBe('100%');
    expect((criar(-3, 12).querySelector('.barra-preenchida') as HTMLElement).style.width).toBe('0%');
  });

  it('sem maximo nao desenha barra', () => {
    const el = criar(5, null);
    expect(el.textContent).toContain('—');
    expect(el.querySelector('.barra-preenchida')).toBeNull();
  });
});
