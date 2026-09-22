import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';

import { App } from './app';
import { routes } from './app.routes';

/**
 * El shell: la barra, sus enlaces y —desde S160— el hueco del selector de curso.
 *
 * <p><b>El {@code provideHttpClientTesting} entra en S160 y NO es adorno.</b> El shell monta
 * {@code CursoBarra}, que pide {@code GET /api/curso} al iniciarse: sin backend de pruebas
 * esa petición sale por el de verdad y falla con status 0 en jsdom. El fichero seguía en
 * verde —la barra degrada a «Curso: no disponible», que es su contrato—, pero a costa de una
 * petición de red real en cada corrida de la suite. Medido con una sonda en esta misma
 * sesión.
 */
describe('App', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  /**
   * Responde el GET de la barra con un curso cualquiera. No se verifica con
   * {@code http.verify()}: lo que este fichero mide es el shell, y exigir aquí el contrato
   * de la barra duplicaría su spec.
   */
  function responderCurso() {
    http.expectOne('/api/curso').flush({
      nombre: '2026/2027',
      archivado: false,
      propuestaSiguiente: '2027/2028',
    });
  }

  it('crea el shell', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('pinta la barra con la marca Educhronos', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.app__marca')?.textContent).toContain('Educhronos');
  });

  it('el hueco del selector monta la barra de curso (S160)', async () => {
    // El cableado, que hasta S160 no comprobaba nadie: el hueco `.app__curso` estuvo vacío
    // desde S127 y quitar el componente de `app.html` no tumbaría ningún otro caso —la
    // barra tiene su propio spec, que la monta suelta—. Esto es lo que ata las dos cosas.
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    responderCurso();
    await fixture.whenStable();
    fixture.detectChanges();

    const hueco = (fixture.nativeElement as HTMLElement).querySelector('.app__curso');
    expect(hueco?.querySelector('app-curso-barra')).not.toBeNull();
    expect(hueco?.textContent).toContain('2026/2027');
  });

  it('los enlaces de navegación están vivos (href resuelta por el router)', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const hrefs = Array.from(compiled.querySelectorAll('.app__nav a'))
      .map((a) => a.getAttribute('href'));
    expect(hrefs).toContain('/configuracion');
    expect(hrefs).toContain('/horario/1');
  });
});
