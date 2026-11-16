import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EscolhaSistema } from './escolha-sistema';

function montar(campanha?: string) {
  TestBed.configureTestingModule({ imports: [EscolhaSistema], providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(EscolhaSistema);
  if (campanha) {
    fixture.componentRef.setInput('campanha', campanha);
  }
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('EscolhaSistema', () => {
  it('mostra tres cartoes: Ordem Paranormal, D&D 5e (2014) e D&D 2024', () => {
    const el = montar();
    const nomes = Array.from(el.querySelectorAll('.cartao-sistema h2')).map((h) => h.textContent?.trim());
    expect(nomes).toEqual(['Ordem Paranormal', 'D&D 5e (2014)', 'D&D 2024']);
  });

  it('os tres cartoes sao links para o formulario de cada sistema', () => {
    const el = montar();
    const links = Array.from(el.querySelectorAll('a.cartao-sistema[href]'));
    expect(links.map((l) => l.getAttribute('href'))).toEqual([
      '/fichas/nova/ordem-paranormal',
      '/fichas/nova/dnd-2014',
      '/fichas/nova/dnd-2024',
    ]);
    expect(links[0].textContent).toContain('Ordem Paranormal');
    expect(links[1].textContent).toContain('D&D 5e (2014)');
    expect(links[2].textContent).toContain('D&D 2024');
  });

  it('com ?campanha=3 os cartoes levam a campanha adiante e o voltar vai para ela', () => {
    const el = montar('3');
    const links = Array.from(el.querySelectorAll('a.cartao-sistema[href]'));
    expect(links.map((l) => l.getAttribute('href'))).toEqual([
      '/fichas/nova/ordem-paranormal?campanha=3',
      '/fichas/nova/dnd-2014?campanha=3',
      '/fichas/nova/dnd-2024?campanha=3',
    ]);
    expect(el.querySelector('a.voltar')?.getAttribute('href')).toBe('/campanhas/3');
  });

  it('sem campanha o voltar vai para as fichas', () => {
    expect(montar().querySelector('a.voltar')?.getAttribute('href')).toBe('/fichas');
  });

  it('nenhum cartao fica desabilitado nem mostra "Em breve"', () => {
    const el = montar();
    expect(el.querySelector('[aria-disabled]')).toBeNull();
    expect(el.textContent).not.toContain('Em breve');
  });
});
