import { TestBed } from '@angular/core/testing';
import { AnelStatus } from './anel-status';

function criar(atual: number | null, maximo: number | null) {
  const fixture = TestBed.createComponent(AnelStatus);
  fixture.componentRef.setInput('rotulo', 'Vida');
  fixture.componentRef.setInput('tipo', 'vida');
  fixture.componentRef.setInput('atual', atual);
  fixture.componentRef.setInput('maximo', maximo);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('AnelStatus', () => {
  it('escreve atual/maximo ao lado e desenha o arco proporcional', () => {
    const el = criar(9, 12);
    expect(el.textContent).toContain('Vida');
    expect(el.textContent).toContain('9/12');
    expect(el.querySelector('circle.arco')?.getAttribute('stroke-dasharray')).toMatch(/^75\.375 /);
  });

  it('o arco nao passa de uma volta quando o atual passa do maximo', () => {
    const el = criar(15, 12);
    expect(el.textContent).toContain('15/12');
    expect(el.querySelector('circle.arco')?.getAttribute('stroke-dasharray')).toMatch(/^100\.5 /);
  });

  it('sem maximo mostra o valor atual (e nao desenha o arco)', () => {
    const el = criar(5, null);
    expect(el.querySelector('.valor')?.textContent?.trim()).toBe('5');
    expect(el.querySelector('circle.arco')).toBeNull();
  });

  it('sem maximo e sem atual mostra traco', () => {
    expect(criar(null, null).querySelector('.valor')?.textContent?.trim()).toBe('—');
  });

  it('o desenho fica fora da leitura de tela; quem le e o texto', () => {
    expect(criar(9, 12).querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('o tipo temporario pinta o arco com a classe temporario', () => {
    const fixture = TestBed.createComponent(AnelStatus);
    fixture.componentRef.setInput('rotulo', 'Temporário');
    fixture.componentRef.setInput('tipo', 'temporario');
    fixture.componentRef.setInput('atual', 4);
    fixture.componentRef.setInput('maximo', 10);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('circle.arco')?.classList).toContain('temporario');
  });
});
