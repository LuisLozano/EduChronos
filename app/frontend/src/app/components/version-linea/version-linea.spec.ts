import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';

import { VersionLinea } from './version-linea';

/**
 * Spec de {@link VersionLinea}: qué se lee en la barra con y sin respuesta de
 * `GET /api/version` (C-version-y-rastro, condición 2, S193). Secuencia propia del fichero
 * desde (1).
 */
describe('VersionLinea', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VersionLinea],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) con respuesta, enseña la versión recibida tal cual', async () => {
    const fixture = TestBed.createComponent(VersionLinea);
    fixture.detectChanges();
    http.expectOne('/api/version').flush({ version: '0.3.0' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent?.trim()).toBe('versión 0.3.0');
  });

  it('(2) si el GET falla, lo dice: «versión no disponible»', async () => {
    const fixture = TestBed.createComponent(VersionLinea);
    fixture.detectChanges();
    http.expectOne('/api/version').flush(
      { message: 'la base no responde' },
      { status: 500, statusText: 'Server Error' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent?.trim()).toBe(
      'versión no disponible',
    );
  });
});
