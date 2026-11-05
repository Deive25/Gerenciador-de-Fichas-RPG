import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EscolhaSistema } from './escolha-sistema';

function montar() {
  TestBed.configureTestingModule({ imports: [EscolhaSistema], providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(EscolhaSistema);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('EscolhaSistema', () => {
  it('mostra tres cartoes: Ordem Paranormal, D&D 5e (2014) e D&D 2024', () => {
    const el = montar();
    const nomes = Array.from(el.querySelectorAll('.cartao-sistema h2')).map((h) => h.textContent?.trim());
    expect(nomes).toEqual(['Ordem Paranormal', 'D&D 5e (2014)', 'D&D 2024']);
  });

  it('so a Ordem e um link, e leva ao formulario de Ordem', () => {
    const el = montar();
    const links = el.querySelectorAll('a.cartao-sistema[href]');
    expect(links.length).toBe(1);
    expect(links[0].getAttribute('href')).toBe('/fichas/nova/ordem-paranormal');
    expect(links[0].textContent).toContain('Ordem Paranormal');
  });

  it('os de D&D ficam desabilitados com "Em breve" e sem link', () => {
    const el = montar();
    const desabilitados = Array.from(el.querySelectorAll('.cartao-sistema[aria-disabled="true"]'));
    expect(desabilitados.length).toBe(2);
    for (const cartao of desabilitados) {
      expect(cartao.textContent).toContain('Em breve');
      expect(cartao.hasAttribute('href')).toBe(false);
    }
    expect(desabilitados[0].textContent).toContain('D&D 5e (2014)');
    expect(desabilitados[1].textContent).toContain('D&D 2024');
  });

  it('a Ordem nao mostra "Em breve"', () => {
    const el = montar();
    expect(el.querySelector('a.cartao-sistema[href]')?.textContent).not.toContain('Em breve');
  });
});
