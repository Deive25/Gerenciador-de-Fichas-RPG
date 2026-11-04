import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ProcessadorDeImagem } from './processador-de-imagem';
import { RecorteRetrato } from './recorte-retrato';

const JPEG = new Blob(['jpeg'], { type: 'image/jpeg' });
const IMAGEM = { tipo: 'imagem falsa' } as unknown as CanvasImageSource;

async function montar(carregar: () => Promise<unknown>) {
  const fechar = vi.fn();
  const recortar = vi.fn(async () => JPEG);
  TestBed.configureTestingModule({
    imports: [RecorteRetrato],
    providers: [
      { provide: MAT_DIALOG_DATA, useValue: { arquivo: new File(['x'], 'foto.png', { type: 'image/png' }) } },
      { provide: MatDialogRef, useValue: { close: fechar } },
      { provide: ProcessadorDeImagem, useValue: { carregar: vi.fn(carregar), recortar, desenharPrevia: vi.fn() } },
    ],
  });
  const fixture = TestBed.createComponent(RecorteRetrato);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement, fechar, recortar };
}

function botao(el: HTMLElement, texto: string): HTMLButtonElement {
  return Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes(texto)) as HTMLButtonElement;
}

describe('RecorteRetrato', () => {
  it('Salvar recorta a janela atual e fecha o dialogo com o JPEG', async () => {
    const { fixture, el, fechar, recortar } = await montar(async () => ({ imagem: IMAGEM, largura: 1000, altura: 1000 }));

    botao(el, 'Salvar').click();
    await fixture.whenStable();

    expect(recortar).toHaveBeenCalledWith(IMAGEM, { x: 100, y: 0, largura: 800, altura: 1000 });
    expect(fechar).toHaveBeenCalledWith(JPEG);
  });

  it('o zoom muda a janela recortada', async () => {
    const { fixture, el, recortar } = await montar(async () => ({ imagem: IMAGEM, largura: 1000, altura: 1000 }));
    const zoom = el.querySelector('input[type="range"][aria-label="Zoom"]') as HTMLInputElement;

    zoom.value = '2';
    zoom.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    botao(el, 'Salvar').click();
    await fixture.whenStable();

    expect(recortar).toHaveBeenCalledWith(IMAGEM, { x: 300, y: 250, largura: 400, altura: 500 });
  });

  it('Cancelar fecha sem resultado e sem recortar', async () => {
    const { el, fechar, recortar } = await montar(async () => ({ imagem: IMAGEM, largura: 1000, altura: 1000 }));

    botao(el, 'Cancelar').click();

    expect(fechar).toHaveBeenCalledWith(undefined);
    expect(recortar).not.toHaveBeenCalled();
  });

  it('imagem que nao abre mostra a mensagem e nao deixa salvar', async () => {
    const { el } = await montar(async () => {
      throw new Error('Não foi possível abrir esta imagem; tente JPG ou PNG');
    });

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Não foi possível abrir esta imagem; tente JPG ou PNG');
    expect(botao(el, 'Salvar').disabled).toBe(true);
  });
});
