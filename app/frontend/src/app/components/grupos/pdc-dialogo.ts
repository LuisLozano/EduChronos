import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Dialog, DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { PdcService } from '../../services/pdc.service';
import { Grupo, PdcRequest } from '../../models/grupo.model';
import { ConfirmarBorrado } from '../confirmar-borrado/confirmar-borrado';

/**
 * En qué estado está la pantalla. UN valor, no cuatro booleanos: cuatro banderas
 * independientes admiten 16 combinaciones, de las que 12 son imposibles —«cargando y
 * con PDC», «sin PDC y en error»—, y el spec no podría afirmar sobre el estado sin
 * enumerar las que no deben darse. Con una unión, el aserto es una igualdad.
 */
export type EstadoPdc = 'cargando' | 'sin-pdc' | 'con-pdc' | 'error';

/**
 * Diálogo del PDC de un grupo ordinario: consulta, alta y borrado del sub-recurso
 * `/api/grupos/{idPadre}/pdc` en una sola pantalla. `DIALOG_DATA` es el GRUPO PADRE
 * —el objeto de la fila pulsada, nunca un PDC y nunca null—, del que salen el id para
 * las tres llamadas y el código para titular.
 *
 * <p>Cierra con `true` si hubo cualquier ESCRITURA con éxito (alta o borrado) y con
 * `false` si el usuario sale sin escribir: es el contrato que espera el `abrirForm` de
 * {@code GrupoLista}, que solo recarga con `true` estricto.
 *
 * <p>Vive junto a los componentes de grupo y no en carpeta propia porque es el mismo
 * dominio: un PDC no es una entidad de catálogo aparte, es algo que cuelga de un grupo.
 *
 * <p><b>CUATRO ESTADOS, y el inicial es {@code 'cargando'}.</b> Es la decisión de forma
 * de este componente. La app es ZONELESS y la respuesta del `ngOnInit` llega un frame
 * después, así que arrancar en `'sin-pdc'` —tentador, porque es el estado más común—
 * pintaría el formulario de alta durante ese hueco a grupos que SÍ tienen PDC: un
 * parpadeo que afirma lo contrario de la verdad justo antes de corregirse. Mientras
 * dura `'cargando'` no se pinta ninguna de las otras ramas.
 *
 * <p><b>El 404 NO es un error aquí: es el estado {@code 'sin-pdc'}.</b> El servicio
 * propaga el `HttpErrorResponse` sin transformarlo precisamente para que esta decisión
 * se tome aquí, que es donde hay contexto. La discriminación es por
 * {@code err.status === 404} y nada más: CUALQUIER otro status (500, 0 de red, 403…)
 * es `'error'`, y en ese estado NO se ofrece el alta. El motivo no es cosmético —con un
 * 500 no se sabe si el grupo tiene PDC o no, y ofrecer el alta a ciegas lleva a un 400
 * de «ya tiene un PDC» que el usuario no puede interpretar—.
 *
 * <p>No hay EDICIÓN, porque el backend no la tiene: {@code PdcController} expone POST,
 * GET y DELETE. Renombrar un PDC es borrarlo y volverlo a crear, y por eso la ficha
 * ofrece «Borrar» y no «Editar».
 */
@Component({
  selector: 'app-pdc-dialogo',
  imports: [ReactiveFormsModule],
  templateUrl: './pdc-dialogo.html',
  styleUrl: './pdc-dialogo.css',
})
export class PdcDialogo implements OnInit {
  private readonly service = inject(PdcService);
  private readonly dialog = inject(Dialog);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);

  /** El grupo ordinario PADRE. Siempre presente: lo pone quien abre el diálogo. */
  protected readonly padre = inject<Grupo>(DIALOG_DATA);

  protected readonly estado = signal<EstadoPdc>('cargando');
  /** El PDC ya cargado. Solo se lee en `'con-pdc'`; en el resto de estados es null. */
  protected readonly pdc = signal<Grupo | null>(null);
  protected readonly guardando = signal(false);
  /** Error de consulta o de escritura. Vacío = sin error. */
  protected readonly error = signal('');

  /**
   * UN control y un solo validador. El `codigo` lo escribe el usuario (D1-3 de S76): no
   * se deriva del padre. Sin patrón ni longitud mínima —el contrato no los tiene— y sin
   * validación async de unicidad: la fuente de verdad es el backend (UNIQUE + la guarda
   * del código de subgrupo derivado) y duplicarla aquí sería una segunda fuente con
   * race condition. Los dos 400 posibles se PRESENTAN cuando llegan.
   */
  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
  });

  /**
   * Consulta el PDC del padre y deriva el estado de la respuesta. Es la única petición
   * del montaje y la que decide qué rama ve el usuario.
   */
  ngOnInit(): void {
    this.service.obtener(this.padre.id).subscribe({
      next: (pdc) => {
        this.pdc.set(pdc);
        this.estado.set('con-pdc');
      },
      error: (err: HttpErrorResponse) => {
        // 404 = "este padre no tiene PDC", que es un ESTADO, no un fallo. Ver el
        // javadoc de clase: la comparación es con el status y no con la verdad del
        // objeto error, porque todo error es "truthy" y eso colapsaría los cuatro
        // estados en dos.
        if (err?.status === 404) {
          this.estado.set('sin-pdc');
          return;
        }
        this.error.set(this.mensaje(
          err, `No se pudo consultar el PDC de ${this.padre.codigo}`));
        this.estado.set('error');
      },
    });
  }

  /**
   * Alta del PDC. En éxito cierra con `true`; en error PRESENTA el mensaje y NO cierra,
   * porque los dos 400 que pueden llegar —código de grupo duplicado y código de
   * subgrupo derivado colisionado— se arreglan escribiendo otro código, sin salir.
   */
  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    const peticion: PdcRequest = this.form.getRawValue();

    this.service.crear(this.padre.id, peticion).subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo crear el PDC'));
        this.guardando.set(false);
      },
    });
  }

  /**
   * Pide confirmación antes de borrar, con el molde {@code ConfirmarBorrado} de la
   * lista. Las líneas AVISAN de lo que el borrado arrastra: el subgrupo automático se
   * va con el PDC, y eso el usuario no puede deducirlo de la pantalla —nunca lo creó
   * él—. El aviso NO reproduce el código derivado de ese subgrupo: esa derivación es
   * una regla del backend y escribirla aquí la convertiría en una segunda fuente de
   * verdad que se desincroniza sola.
   */
  protected borrar(): void {
    const actual = this.pdc();
    if (actual === null) {
      return;
    }
    const lineas = [
      `¿Borrar el PDC ${actual.codigo} de ${this.padre.codigo}?`,
      'Se borrará también su subgrupo automático, que es la población con la que el'
        + ' PDC entra en las actividades.',
    ];
    this.dialog
      .open<boolean, string[]>(ConfirmarBorrado, { data: lineas })
      .closed.subscribe((confirmado) => {
        // backdrop y Escape emiten undefined: solo `true` estricto borra.
        if (confirmado === true) {
          this.confirmarBorrado();
        }
      });
  }

  /**
   * El DELETE ya confirmado. En error PRESENTA y NO cierra: el 409 («alguna plaza usa
   * el subgrupo») es informativo y el usuario tiene que poder leerlo y salir por su
   * pie, no encontrarse el diálogo cerrado sin saber qué pasó.
   */
  private confirmarBorrado(): void {
    this.guardando.set(true);
    this.error.set('');
    this.service.borrar(this.padre.id).subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(
          err, `No se pudo borrar el PDC de ${this.padre.codigo}`));
        this.guardando.set(false);
      },
    });
  }

  /** Salida sin escribir: la lista no recarga. */
  protected cerrar(): void {
    this.ref.close(false);
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón que `GrupoForm` y `GrupoLista`,
   * copiado con texto propio; NO extraído a utilidad compartida a propósito: hacerlo
   * tocaría los componentes de H1 (D-F8.6, cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
