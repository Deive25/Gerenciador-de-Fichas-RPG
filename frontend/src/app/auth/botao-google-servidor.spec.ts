import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { GoogleAccountsId, GoogleIdentity } from '../core/auth/google-identity';
import { EstadoDoServidor } from '../core/servidor/estado-do-servidor';
import { BotaoGoogle } from './botao-google';

describe('BotaoGoogle com o servidor acordando', () => {
  const idFalso: GoogleAccountsId = { initialize: () => undefined, renderButton: () => undefined };

  function montar() {
    const aguardando = signal(true);
    let liberar!: () => void;
    const liberado = new Promise<void>((resolver) => (liberar = resolver));
    const carregar = vi.fn(() => Promise.resolve(idFalso));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GoogleIdentity, useValue: { carregar } },
        { provide: EstadoDoServidor, useValue: { aguardando, quandoLiberado: () => liberado } },
      ],
    });
    const fixture = TestBed.createComponent(BotaoGoogle);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement, aguardando, liberar, carregar };
  }

  it('mostra "aguardando o servidor" e não baixa o script do Google', async () => {
    const { fixture, el, carregar } = montar();
    await fixture.whenStable();
    expect(el.querySelector('.aviso-servidor')?.textContent).toMatch(/aguardando o servidor/i);
    expect(carregar).not.toHaveBeenCalled();
  });

  it('quando a API responde, tira o texto e carrega o botão', async () => {
    const { fixture, el, aguardando, liberar, carregar } = montar();
    await fixture.whenStable();
    aguardando.set(false);
    liberar();
    await new Promise((r) => setTimeout(r));
    fixture.detectChanges();
    expect(carregar).toHaveBeenCalledTimes(1);
    expect(el.querySelector('.aviso-servidor')).toBeNull();
  });
});
