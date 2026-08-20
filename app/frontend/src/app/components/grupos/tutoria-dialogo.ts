import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { TutoriaService } from '../../services/tutoria.service';
import { ProfesorService } from '../../services/profesor.service';
import { Grupo } from '../../models/grupo.model';
import { Profesor } from '../../models/profesor.model';
import { Tutoria, TutoriaRequest } from '../../models/tutoria.model';

/**
 * En qué estado está la pantalla. UN valor, no banderas sueltas: mismo criterio que
 * {@code EstadoPdc}, donde tres booleanos independientes admitirían combinaciones
 * imposibles —«cargando y cargado»— que el spec tendría que enumerar para negarlas.
 *
 * <p>TRES y no cuatro: aquí NO hay un estado «sin tutoría» hermano del `'sin-pdc'` de
 * {@code PdcDialogo}. Ver el javadoc de clase: el vacío no es un estado de carga, es un
 * valor del formulario.
 */
export type EstadoTutoria = 'cargando' | 'cargado' | 'error';

/**
 * Diálogo de la TUTORÍA de un grupo: consulta y reemplazo del sub-recurso
 * `/api/grupos/{idGrupo}/tutoria` (§4.1, invariante I4). `DIALOG_DATA` es el GRUPO —el
 * objeto de la fila pulsada, nunca null—, del que salen el id para las dos llamadas y
 * el código para titular. Molde de {@code PdcDialogo}: entidad directa en el
 * `DIALOG_DATA`, estado en una unión de literales, `mensaje()` copiado con texto propio.
 *
 * <p>Cierra con `true` si hubo una escritura con éxito y sin argumento si el usuario
 * sale sin escribir: el consumidor recarga solo con `true` estricto.
 *
 * <p><b>EL VACÍO NO ES UN 404, Y ESTA ES LA DIFERENCIA CON {@code PdcDialogo}.</b> Aquel
 * deriva su estado «sin PDC» de un `err.status === 404`; aquí eso sería un error de
 * lectura del contrato. El `GET` del sub-recurso responde 200 con LISTA VACÍA cuando el
 * grupo no tiene tutoría (`TutoriaService.java:75,81`, literal: «Lista vacía = grupo sin
 * tutoría (no es un 404)»), y reserva el 404 para «no existe grupo con ese id». Por eso
 * «no hay tutor» se deriva de `length === 0` sobre la respuesta, y un 404 lleva al
 * estado `'error'` como cualquier otro fallo: significa que el grupo no existe, no que
 * le falte tutor, y pintarlo como «sin tutor» taparía el fallo real.
 *
 * <p><b>DOS FUENTES DE RED Y UN SOLO {@code forkJoin}: construcción NUEVA en el
 * proyecto.</b> No había precedente —`forkJoin`/`combineLatest` no aparecían en
 * `app/frontend/src`—, así que la desviación se documenta aquí. El motivo es el GATING
 * DE ESTADOS, heredado de {@code PdcDialogo}: el formulario no se pinta hasta que
 * ambas respuestas están, y con dos suscripciones sueltas «cargado» habría que
 * derivarlo contando respuestas a mano, con un contador que es estado invisible y que
 * el spec no podría afirmar de una vez.
 *
 * <p><b>Lo que este {@code forkJoin} NO arregla, para que nadie se lo atribuya.</b> No
 * hace falta para conservar la preselección del tutor: el `<select>` único de Reactive
 * Forms RECONCILIA su valor cuando llegan las opciones, aunque el `setValue` haya
 * corrido antes de que existiera ninguna `<option>`. Está medido en el molde
 * (`grupo-form.ts:87-90` y su spec (8), que hace el `setValue` en el constructor y
 * comprueba `selectedIndex === 2`). Quien sustituya este `forkJoin` por dos
 * suscripciones perderá el gating, no la preselección.
 *
 * <p><b>LOS CO-TUTORES SE CONSERVAN, Y ESA ES LA LÓGICA DELICADA DE ESTA PANTALLA.</b>
 * El `PUT` es un REEMPLAZO TOTAL (`TutoriaService.java:85-88`): lo que no se reenvía se
 * borra. Esta pantalla solo EDITA el `TUTOR_PRINCIPAL` —dar de alta o de baja co-tutores
 * queda fuera de alcance—, así que al guardar reenvía los `CO_TUTOR` que llegaron TAL
 * CUAL, delante o detrás del principal. Omitirlos no sería «no tocarlos»: sería
 * borrarlos en silencio, y el usuario no vería el destrozo hasta abrir de nuevo. Se
 * pintan en solo lectura precisamente para que se vea que están y que van a sobrevivir.
 *
 * <p>El desplegable NO reordena el profesorado: llega en el orden del backend, mismo
 * criterio que el de niveles de {@code GrupoForm}.
 */
@Component({
  selector: 'app-tutoria-dialogo',
  imports: [ReactiveFormsModule],
  templateUrl: './tutoria-dialogo.html',
  styleUrl: './tutoria-dialogo.css',
})
export class TutoriaDialogo implements OnInit {
  private readonly service = inject(TutoriaService);
  private readonly profesorService = inject(ProfesorService);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);

  /** El grupo cuya tutoría se edita. Siempre presente: lo pone quien abre el diálogo. */
  protected readonly grupo = inject<Grupo>(DIALOG_DATA);

  protected readonly estado = signal<EstadoTutoria>('cargando');
  protected readonly guardando = signal(false);
  /** Error de consulta o de escritura. Vacío = sin error. */
  protected readonly error = signal('');

  /** Opciones del desplegable, en el orden en que llegan del backend. */
  protected readonly profesorado = signal<Profesor[]>([]);

  /**
   * Los `CO_TUTOR` tal como llegaron del `GET`. Se pintan en SOLO LECTURA y se reenvían
   * intactos en el `PUT`; esta pantalla no los crea ni los borra. Ver el javadoc de
   * clase: son la mitad de la lista que el reemplazo total destruiría si se omitiera.
   */
  protected readonly coTutores = signal<Tutoria[]>([]);

  /**
   * UN control, y SIN {@code Validators.required}: `''` no es un formulario a medio
   * rellenar, es el valor «sin tutor principal», que el backend acepta —la escritura
   * hace cumplir «como mucho un principal», nunca «exactamente uno»
   * (`TutoriaService.java:51-55`)—. Un `required` aquí haría imposible el gesto de
   * QUITAR el tutor, que es justamente una de las cosas que esta pantalla existe para
   * hacer.
   */
  protected readonly form = this.fb.nonNullable.group({
    principal: [''],
  });

  /**
   * Pide la tutoría del grupo y el profesorado, y no deriva `'cargado'` hasta tener las
   * DOS. El `forkJoin` emite una sola vez con ambas o falla con la primera que falle:
   * un fallo en cualquiera de las dos lleva a `'error'`, porque sin profesorado el
   * desplegable no se puede ofrecer y sin tutoría no se sabe qué preseleccionar.
   */
  ngOnInit(): void {
    forkJoin({
      tutoria: this.service.obtener(this.grupo.id),
      profesores: this.profesorService.listar(),
    }).subscribe({
      next: ({ tutoria, profesores }) => {
        this.profesorado.set(profesores);
        // Los dos roles se separan aquí y no en la plantilla: la plantilla pinta, no
        // decide. `find` y no `filter[0]`: el backend garantiza como mucho un principal.
        this.coTutores.set(tutoria.filter((t) => t.rol === 'CO_TUTOR'));
        const principal = tutoria.find((t) => t.rol === 'TUTOR_PRINCIPAL');
        // `''` cuando no hay: casa la opción «— sin tutor —» del desplegable. Derivado
        // de la LISTA, nunca de un status: ver el javadoc de clase.
        this.form.controls.principal.setValue(principal ? principal.profesor : '');
        this.estado.set('cargado');
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(
          err, `No se pudo consultar la tutoría de ${this.grupo.codigo}`));
        this.estado.set('error');
      },
    });
  }

  /**
   * Reemplaza la tutoría del grupo. El cuerpo es el principal elegido —si lo hay— MÁS
   * todos los co-tutores que llegaron, sin tocar. Sin principal y sin co-tutores el
   * cuerpo es `[]`, que es como se borra la tutoría: no hay DELETE en el sub-recurso.
   *
   * <p>En error PRESENTA el mensaje y NO cierra: el usuario tiene que poder leerlo y
   * decidir. Contempla el 404 DE ESCRITURA, que es contraintuitivo —un código de
   * profesor inexistente da 404, no 400 (`TutoriaService.java:147-152`)—; el texto lo
   * compone el backend y viaja en `message`.
   */
  protected guardar(): void {
    this.guardando.set(true);
    this.error.set('');

    const codigo = this.form.getRawValue().principal;
    const cuerpo: TutoriaRequest[] = [
      ...(codigo ? [{ profesor: codigo, rol: 'TUTOR_PRINCIPAL' as const }] : []),
      ...this.coTutores(),
    ];

    this.service.reemplazar(this.grupo.id, cuerpo).subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo guardar la tutoría'));
        this.guardando.set(false);
      },
    });
  }

  /** Salida sin escribir: el consumidor no recarga. */
  protected cerrar(): void {
    this.ref.close();
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón que `PdcDialogo` y `GrupoForm`,
   * copiado con texto propio; NO extraído a utilidad compartida a propósito: hacerlo
   * tocaría los componentes de H1 (D-F8.6, cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
