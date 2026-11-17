import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationInitStatus } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { environment } from '../environments/environment';
import { appConfig } from './app.config';

describe('appConfig', () => {
  it('ao abrir o app, confere o liveness da API', async () => {
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    await TestBed.inject(ApplicationInitStatus).donePromise;
    const req = TestBed.inject(HttpTestingController).expectOne(`${environment.apiUrl}/actuator/health/liveness`);
    expect(req.request.method).toBe('GET');
  });
});
