import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiError } from '../core/api-error';
import { RolagensApi } from './rolagens-api';

describe('RolagensApi', () => {
  let api: RolagensApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(RolagensApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('pede as 50 últimas, sem cursor', () => {
    api.historico(3).subscribe();
    const req = http.expectOne('http://localhost:8080/campanhas/3/rolagens?limite=50');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('manda o cursor antes', () => {
    api.historico(3, 41).subscribe();
    http.expectOne('http://localhost:8080/campanhas/3/rolagens?limite=50&antes=41').flush([]);
  });

  it('o erro sai como ApiError', () => {
    let erro: ApiError | undefined;
    api.historico(3).subscribe({ error: (e: ApiError) => (erro = e) });
    http.expectOne('http://localhost:8080/campanhas/3/rolagens?limite=50').flush(null, { status: 404, statusText: 'x' });
    expect(erro?.status).toBe(404);
  });
});
