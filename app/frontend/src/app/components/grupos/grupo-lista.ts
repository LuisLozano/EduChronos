import { Component, OnInit, inject, signal } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { GrupoService } from '../../services/grupo.service';
import { Grupo } from '../../models/grupo.model';
import { GrupoForm } from './grupo-form';
import { PdcDialogo } from './pdc-dialogo';
import { TutoriaDialogo } from './tutoria-dialogo';
import { ConfirmarBorrado } from '../confirmar-borrado/confirmar-borrado';

/** El único tipo que admite acciones en esta pantalla. Ver el javadoc de la clase. */
const TIPO_ORDINARIO = 'ORDINARIO';

/**
 * Etiqueta legible por valor del enum. Mapa y no `switch` en plantilla: la plantilla
 * pinta, no decide. Lo que NO está aquí se pinta crudo, a propósito.
 */
const ETIQUETAS_TIPO = new Map<string, string>([
  [TIPO_ORDINARIO, 'Ordinario'],
  ['DIVERSIFICACION_PDC', 'PDC'],
]);

/**
 * Lista del catálogo de grupos administrativos: carga en init, tabla con acciones por
 * fila, alta/edición en diálogo, borrado con confirmación previa. La escritura vive en
 * `GrupoForm` (diálogo); esta lista lo abre y recarga tras un guardado.
 *
 * <p>TRES COLUMNAS: Código, Nivel y Tipo. La de `tipo` estuvo omitida mientras la
 * pantalla solo mostraba ordinarios —una columna con el mismo valor en todas las filas
 * no distingue nada—, y hoy DISCRIMINA: `GET /api/grupos` hace `findAll()` sin filtrar,
 * así que los PDC creados por el sub-recurso salen aquí junto a los ordinarios. Se
 * pinta con etiqueta legible ({@link #etiquetaTipo}), no con la constante cruda; un
 * valor que no esté en el mapa se muestra TAL CUAL, para que un `VIRTUAL_OPTATIVA`
 * futuro se vea en vez de desaparecer.
 *
 * <p>TRES ACCIONES SOLO PARA LOS ORDINARIOS —Editar, Borrar y PDC—. Una fila
 * `DIVERSIFICACION_PDC` no ofrece ninguna de las tres, porque acabarían en un error que
 * el usuario no puede resolver: Editar da 400 (la guarda que impide degradar un PDC a
 * ordinario por el PUT plano), Borrar da 409 (su subgrupo mono-Di lo retiene) y PDC
 * daría 400 (el sub-recurso exige un padre ORDINARIO; un PDC no cuelga de otro PDC).
 * No se pierde ninguna capacidad al esconderlas: el backend NO tiene edición de PDC
 * —el sub-recurso es alta/consulta/borrado— y su borrado vive en el diálogo del PADRE,
 * que es desde donde se gestiona todo el ciclo. Un tipo DESCONOCIDO se trata como no
 * ordinario: sin esas tres acciones, que es el lado seguro.
 *
 * <p><b>Y UNA CUARTA, «Tutoría», EN TODAS LAS FILAS SIN EXCEPCIÓN.</b> Es la única que
 * queda FUERA del filtro por tipo, y no por descuido: un PDC HEREDA el
 * `TUTOR_PRINCIPAL` de su padre en el alta (`PdcService.java:110`) y puede cambiarlo
 * después —el sub-recurso `/{id}/tutoria` acepta cualquier grupo, sin exigir que sea
 * ordinario—. Esconderla en las filas PDC dejaría sin editar justo el caso que la
 * herencia crea: un grupo con tutor puesto por el sistema y ninguna forma de tocarlo.
 * Un tipo DESCONOCIDO también la ofrece, por el mismo motivo: la tutoría no depende del
 * tipo del grupo.
 *
 * <p>El 409 de borrado sí es rico aquí: un grupo con subgrupos o con hijos PDC no se
 * borra, y el backend nombra cuántos de cada tipo lo impiden.
 */
@Component({
  selector: 'app-grupo-lista',
  templateUrl: './grupo-lista.html',
  styleUrl: './grupo-lista.css',
})
export class GrupoLista implements OnInit {
  private readonly service = inject(GrupoService);
  private readonly dialog = inject(Dialog);

  protected readonly grupos = signal<Grupo[]>([]);
  protected readonly cargando = signal(false);
  /** Error de la última operación de lista o borrado. Vacío = sin error. */
  protected readonly error = signal('');

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.service.listar().subscribe({
      next: (lista) => {
        this.grupos.set(lista);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar la lista de grupos'));
        this.cargando.set(false);
      },
    });
  }

  protected nuevo(): void {
    this.abrirForm(null);
  }

  protected editar(grupo: Grupo): void {
    this.abrirForm(grupo);
  }

  /** Abre el formulario en diálogo; recarga si se guardó (cierre con `true`). */
  private abrirForm(grupo: Grupo | null): void {
    this.dialog
      .open<boolean, Grupo | null>(GrupoForm, { data: grupo })
      .closed.subscribe((guardado) => {
        // backdrop y Escape emiten undefined: solo `true` estricto recarga.
        if (guardado === true) {
          this.cargar();
        }
      });
  }

  /**
   * Abre el diálogo del PDC de ESTA fila. El `data` es el grupo PADRE completo: el
   * diálogo saca de él el id para sus tres llamadas y el código para titularse.
   *
   * <p>Recarga con la MISMA regla que {@link #abrirForm} y por el mismo motivo: el
   * diálogo cierra con `true` si hubo alta o borrado del PDC —y entonces la columna
   * Tipo de la tabla ha cambiado—, y con `false` o `undefined` si el usuario salió sin
   * escribir. El `=== true` estricto no se relaja: `closed` emite `undefined` al cerrar
   * por backdrop o Escape, y un `!== undefined` recargaría también en ese caso.
   */
  protected pdc(grupo: Grupo): void {
    this.dialog
      .open<boolean, Grupo>(PdcDialogo, { data: grupo })
      .closed.subscribe((cambiado) => {
        if (cambiado === true) {
          this.cargar();
        }
      });
  }

  /**
   * Abre el diálogo de tutoría de ESTA fila, con el grupo completo como `data` (mismo
   * molde que {@link #pdc}: entidad directa, no envuelta; el diálogo saca de ella el id
   * para sus dos llamadas y el código para titularse).
   *
   * <p><b>NO SE SUSCRIBE A `closed`, y por tanto NO RECARGA.</b> Es la diferencia con
   * {@link #pdc} y {@link #abrirForm}, y es deliberada: esta tabla no pinta NINGÚN dato
   * de tutoría —sus columnas son Código, Nivel y Tipo—, así que tras guardar no hay nada
   * que refrescar y un `cargar()` sería un GET cuyo resultado se pintaría idéntico.
   * `TutoriaDialogo` sí cierra con `true` cuando escribe, porque ese es su contrato con
   * cualquier consumidor; aquí simplemente no se consume.
   *
   * <p>Si algún día la lista muestra el tutor de cada grupo, ESTE es el punto donde
   * volvería el `.closed.subscribe(...)` con el `=== true` estricto de las otras dos.
   */
  protected tutoria(grupo: Grupo): void {
    this.dialog.open<boolean, Grupo>(TutoriaDialogo, { data: grupo });
  }

  /**
   * ¿Ofrece esta fila las acciones de ordinario (Editar, Borrar, PDC)? NO gobierna el
   * botón de tutoría, que se pinta siempre; ver el javadoc de la clase.
   */
  protected esOrdinario(grupo: Grupo): boolean {
    return grupo.tipo === TIPO_ORDINARIO;
  }

  /** Etiqueta legible del tipo, o el valor crudo si no está en el mapa. */
  protected etiquetaTipo(tipo: string): string {
    return ETIQUETAS_TIPO.get(tipo) ?? tipo;
  }

  protected borrar(grupo: Grupo): void {
    const lineas = [`¿Borrar el grupo ${grupo.codigo}?`];
    this.dialog
      .open<boolean, string[]>(ConfirmarBorrado, { data: lineas })
      .closed.subscribe((confirmado) => {
        if (confirmado === true) {
          this.confirmarBorrado(grupo);
        }
      });
  }

  private confirmarBorrado(grupo: Grupo): void {
    this.error.set('');
    this.service.borrar(grupo.id).subscribe({
      next: () => this.cargar(),
      error: (err: HttpErrorResponse) => {
        // 409 = referencias entrantes. El backend compone el texto rico
        // ("No se puede borrar: referenciada por 2 subgrupo(s), 1 grupo(s) hijo(s)") y
        // viaja en `message` porque server.error.include-message=always. El degradado,
        // si no viajara, dice al menos qué pasó y con qué status.
        this.error.set(this.mensaje(err, `No se pudo borrar el grupo ${grupo.codigo}`));
      },
    });
  }

  /**
   * Traduce error Http a texto de usuario: mensaje del servidor primero
   * (`message`, luego `error`), degradado con status si no hay. Copiado del
   * patrón de `asignatura-lista.mensaje()` con texto propio; NO extraído a utilidad
   * compartida a propósito: hacerlo tocaría horario-view (D-F8.6, H1 cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
