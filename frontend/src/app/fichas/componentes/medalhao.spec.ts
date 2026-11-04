import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RetratoCache } from '../retrato-cache';
import { DEGRADES, Medalhao, degradeDe, iniciais } from './medalhao';

describe('iniciais', () => {
  it('junta a primeira letra da primeira e da ultima palavra', () => {
    expect(iniciais('Lia Fontaine')).toBe('LF');
    expect(iniciais('Ana Maria da Silva')).toBe('AS');
  });

  it('ignora pontuacao e espacos extras', () => {
    expect(iniciais('Prof. Hale')).toBe('PH');
    expect(iniciais('  kai  ')).toBe('K');
  });

  it('nome vazio vira interrogacao', () => {
    expect(iniciais('   ')).toBe('?');
    expect(iniciais('')).toBe('?');
  });
});

describe('degradeDe', () => {
  it('as sementes 0 a 5 dao os 6 degrades, todos diferentes', () => {
    const usados = [0, 1, 2, 3, 4, 5].map(degradeDe);
    expect(new Set(usados).size).toBe(6);
    expect(usados).toEqual([...DEGRADES]);
  });

  it('a mesma ficha tem sempre a mesma cor, inclusive com id negativo', () => {
    expect(degradeDe(7)).toBe(degradeDe(1));
    expect(degradeDe(-1)).toBe(degradeDe(1));
  });
});

describe('Medalhao', () => {
  let url: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    url = vi.fn(() => of('blob:x'));
    TestBed.configureTestingModule({ providers: [{ provide: RetratoCache, useValue: { url } }] });
  });

  function criar(retratoVersao: number | null) {
    const fixture = TestBed.createComponent(Medalhao);
    fixture.componentRef.setInput('nome', 'Lia Fontaine');
    fixture.componentRef.setInput('semente', 7);
    fixture.componentRef.setInput('retratoVersao', retratoVersao);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('com retrato mostra a imagem com texto alternativo e entra na leitura de tela', () => {
    const { el } = criar(100);
    const img = el.querySelector('img') as HTMLImageElement;
    expect(img.getAttribute('src')).toBe('blob:x');
    expect(img.getAttribute('alt')).toBe('Retrato de Lia Fontaine');
    expect(el.hasAttribute('aria-hidden')).toBe(false);
    expect(url).toHaveBeenCalledWith(7, 100);
  });

  it('sem retrato mostra as iniciais e nao pede imagem', () => {
    const { el } = criar(null);
    expect(el.querySelector('img')).toBeNull();
    expect(el.textContent?.trim()).toBe('LF');
    expect(el.getAttribute('aria-hidden')).toBe('true');
    expect(url).not.toHaveBeenCalled();
  });

  it('se a imagem falhar ao carregar, volta para as iniciais', () => {
    const { fixture, el } = criar(100);
    (el.querySelector('img') as HTMLImageElement).dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(el.querySelector('img')).toBeNull();
    expect(el.textContent?.trim()).toBe('LF');
  });

  it('mostra as iniciais sobre o degrade da ficha e fica fora da leitura de tela', () => {
    const fixture = TestBed.createComponent(Medalhao);
    fixture.componentRef.setInput('nome', 'Lia Fontaine');
    fixture.componentRef.setInput('semente', 7);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent?.trim()).toBe('LF');
    expect(el.getAttribute('aria-hidden')).toBe('true');
    expect(el.style.getPropertyValue('--degrade')).toBe(degradeDe(7));
  });

  it('o tamanho grande muda a classe', () => {
    const fixture = TestBed.createComponent(Medalhao);
    fixture.componentRef.setInput('nome', 'Lia');
    fixture.componentRef.setInput('semente', 1);
    fixture.componentRef.setInput('tamanho', 'grande');
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).classList).toContain('grande');
  });
});
