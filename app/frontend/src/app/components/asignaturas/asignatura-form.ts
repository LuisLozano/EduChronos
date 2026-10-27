import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { AsignaturaService } from '../../services/asignatura.service';
import { NivelService } from '../../services/nivel.service';
import { Asignatura, AsignaturaRequest } from '../../models/asignatura.model';
import { Nivel } from '../../models/nivel.model';

/**
 * Alta y edición de una asignatura en un mismo componente, presentado en diálogo.
 * `DIALOG_DATA` es la asignatura a editar, o `null` para alta. Cierra con `true`
 * si se guardó (la lista recarga), con `undefined` si se canceló.
 *
 * <p>TERCER formulario del catálogo, y el CASO PLANO del molde: replica lo que fijó
 * {@code ProfesorForm} (S101) sin necesitar NINGUNA de las dos extensiones que S102
 * añadió en {@code AulaForm} —ni campo de enumerado ni opcionales de verdad—, porque
 * el contrato era el mismo par `(codigo, nombreCompleto)` con ambos obligatorios (hasta S250). Su
 * valor es justamente ese: comprobar que el molde se aplica sin estirarlo.
 *
 * <p>`nonNullable` evita el `| null` de FormControl en TS y encaja con los
 * Validators.required.
 *
 * <p>La unicidad de `codigo` NO se valida aquí async a propósito: la fuente de
 * verdad es el backend (UNIQUE en esquema + findByCodigo). Un async validator
 * duplicaría esa comprobación con una race condition. El 400 de código duplicado
 * se PRESENTA cuando llega, no se anticipa.
 *
 * <p>FUERA DE ALCANCE en S103: el sub-recurso `aulas-compatibles` que
 * {@code AsignaturaController} expone en `GET/PUT /{id}/aulas-compatibles`. Estirar
 * el molde con un sub-recurso es objeto de sesión propia.
 *
 * <p><b>Código corto y nivel</b> (S250, C-asignatura-con-nivel). Se pide el código CORTO y el
 * nivel, y el código de la asignatura lo compone el backend: el corto, un guion y el código del
 * nivel (`Biol-3ESO`), o el corto solo sin nivel. Una línea de solo lectura enseña el código que
 * resultará con la misma regla, para que no sorprenda en la lista. El desplegable pide los
 * niveles en `ngOnInit`, como `GrupoForm`, y arranca en «— sin nivel —», que viaja como `null`.
 */
@Component({
  selector: 'app-asignatura-form',
  imports: [ReactiveFormsModule],
  templateUrl: './asignatura-form.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './asignatura-form.css',
})
export class AsignaturaForm implements OnInit {
  private readonly service = inject(AsignaturaService);
  private readonly nivelService = inject(NivelService);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
  private readonly editando = inject<Asignatura | null>(DIALOG_DATA);

  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly esEdicion = this.editando !== null;

  /** Los niveles del desplegable, en el orden en que los sirve `/api/niveles`. */
  protected readonly niveles = signal<Nivel[]>([]);

  /** `nivel` vale '' para «— sin nivel —»; {@link guardar} lo traduce a `null`. */
  protected readonly form = this.fb.nonNullable.group({
    codigoCorto: ['', Validators.required],
    nivel: [''],
    nombreCompleto: ['', Validators.required],
  });

  /** El código que tendrá la asignatura con lo que hay escrito. */
  protected readonly codigoResultante = signal('');

  constructor() {
    if (this.editando) {
      this.form.setValue({
        codigoCorto: this.editando.codigoCorto,
        nivel: this.editando.nivel ?? '',
        nombreCompleto: this.editando.nombreCompleto,
      });
    }
    this.actualizarCodigo();
    this.form.valueChanges.subscribe(() => this.actualizarCodigo());
  }

  /** Como `GrupoForm`: sin niveles no se puede elegir uno, y callarlo dejaría el desplegable mudo. */
  ngOnInit(): void {
    this.nivelService.listar().subscribe({
      next: (lista) => this.niveles.set(lista),
      error: (err: HttpErrorResponse) =>
        this.error.set(this.mensaje(err, 'No se pudieron cargar los niveles')),
    });
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    const valor = this.form.getRawValue();
    const req: AsignaturaRequest = {
      codigoCorto: valor.codigoCorto,
      nombreCompleto: valor.nombreCompleto,
      nivel: valor.nivel === '' ? null : valor.nivel,
    };

    const peticion = this.editando
      ? this.service.editar(this.editando.id, req)
      : this.service.crear(req);

    peticion.subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        // 400 = código duplicado (los required descartan el vacío). El backend
        // compone "Ya existe una asignatura con codigo MAT8" y viaja en `message`.
        this.error.set(this.mensaje(err, 'No se pudo guardar la asignatura'));
        this.guardando.set(false);
      },
    });
  }

  protected cancelar(): void {
    this.ref.close();
  }

  /** La regla del backend (`Asignatura.componerCodigo`): `corto-nivel`, o el corto solo. */
  private actualizarCodigo(): void {
    const { codigoCorto, nivel } = this.form.getRawValue();
    this.codigoResultante.set(nivel === '' ? codigoCorto : `${codigoCorto}-${nivel}`);
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón que `AsignaturaLista` y
   * `horario-view.mensaje()`, copiado con texto propio (no compartido: tocaría H1).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
