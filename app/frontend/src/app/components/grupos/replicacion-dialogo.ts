import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { ReplicacionService } from '../../services/replicacion.service';
import { GrupoService } from '../../services/grupo.service';
import { Grupo } from '../../models/grupo.model';
import { PlanReplicacion, ReplicacionRequest, Via } from '../../models/replicacion.model';

/**
 * En qué estado está la pantalla. UN valor, no banderas: siete booleanos independientes
 * admiten 128 combinaciones de las que 121 son imposibles —«enviando y deshaciendo»—, y el
 * spec no podría afirmar sobre el estado sin enumerar las que no deben darse. Con una
 * unión, el aserto es una igualdad. Mismo criterio que {@code EstadoPdc}.
 *
 * <p>`'cargando'` cubre las DOS esperas de red que preceden a una decisión —la lista de
 * hermanos del montaje y el plan del hermano elegido— porque en las dos la pantalla no
 * puede pintar nada que no sea mentira. No se desdobla en dos estados: la plantilla pinta
 * lo mismo y ningún caso los distingue.
 */
export type EstadoReplicacion =
  | 'cargando'
  | 'eligiendo'
  | 'decidiendo'
  | 'enviando'
  | 'hecho'
  | 'deshaciendo'
  | 'error';

/** El único tipo de grupo que se puede replicar, y el único del que se puede replicar. */
const TIPO_ORDINARIO = 'ORDINARIO';

/**
 * Valor con el que un desplegable de reparto dice «no matricular aquí». NO es `''`: `''`
 * es «aún no has contestado», y confundirlos es exactamente lo que haría que «Aceptar» se
 * habilitase con decisiones sin tomar. Se traduce a `plaza: null` al componer el cuerpo,
 * que es respuesta legítima del contrato.
 */
const SIN_PLAZA = 'sin-plaza';

/** Una vía ofrecida en un desplegable, con su ordinal dentro de SU bloque. */
interface OpcionVia {
  readonly via: Via;
  readonly ordinal: number;
}

/** Las vías de UN bloque, agrupadas bajo su actividad para el `<optgroup>`. */
interface BloqueDeCuestion {
  readonly actividad: string;
  readonly opciones: readonly OpcionVia[];
}

/** UNA decisión pendiente: un espejo de reparto y las vías entre las que puede ir. */
interface Cuestion {
  readonly espejo: string;
  readonly bloques: readonly BloqueDeCuestion[];
}

/**
 * Diálogo de REPLICACIÓN de un grupo ordinario: elegir hermano, ver lo que la replicación
 * haría, decidir el reparto, ejecutarla y —si no convence— deshacerla, todo sobre el
 * sub-recurso `/api/grupos/{id}/replicacion` (Bloques S139, S140 y S141,
 * `C-alta-por-pantalla`). `DIALOG_DATA` es el GRUPO NUEVO —el objeto de la fila pulsada,
 * nunca null—, del que salen el id para las tres llamadas y el código para titular.
 *
 * <p>Molde de {@code PdcDialogo}: entidad directa en el `DIALOG_DATA`, estado en una unión
 * de literales, un error PRESENTA y NO CIERRA, `mensaje()` copiado con texto propio. La
 * CAJA, en cambio, se calca de {@code ActividadForm} y no de aquél: es el único diálogo del
 * proyecto con una lista de longitud variable dentro, y el único que ya resolvió el
 * desbordamiento con `max-height` y scroll propio.
 *
 * <p><b>Cierra con `true` si se ESCRIBIÓ o si se DESHIZO, y con `false` si el usuario salió
 * sin tocar nada.</b> Los dos primeros van juntos a propósito: en ambos han cambiado los
 * subgrupos del grupo, y la lista que abre este diálogo pinta una columna que depende de
 * ellos. Deshacer no es «cancelar»: es una segunda escritura.
 *
 * <p><b>EL ESTADO INICIAL ES `'cargando'`, NO `'eligiendo'`.</b> Misma razón que el
 * `'cargando'` de {@code PdcDialogo}: la app es ZONELESS y la lista de hermanos llega un
 * frame después, así que arrancar en `'eligiendo'` pintaría un desplegable VACÍO durante ese
 * hueco, que afirma «este grupo no tiene hermanos» justo antes de desmentirse.
 *
 * <p><b>TRES FILTROS SOBRE LA LISTA DE HERMANOS, y uno de ellos suple al servidor.</b> Se
 * ofrecen los ordinarios del mismo nivel distintos del propio grupo. Los dos primeros
 * filtros reproducen guardas que el servidor SÍ tiene; el de tipo no: `analizar` valida el
 * tipo del GRUPO pero no el del HERMANO, así que un PDC del mismo nivel es un hermano que el
 * backend acepta y que revienta después, dentro de `derivarEspejos`, con un 400 sobre
 * prefijos de código que no explica nada de lo que el usuario hizo mal.
 *
 * <p><b>«Nada que copiar» se para AQUÍ o no lo para nadie.</b> Sobre un hermano sin
 * subgrupos el servidor responde 200 al `GET` con las tres listas vacías y 201 al `POST`,
 * o sea un éxito que no ha creado nada. La condición es que no haya NI espejos NI bloques
 * ({@link #nadaQueCopiar}); no basta con «cero bloques». Un plan con espejos y cero bloques
 * NO se bloquea: es el hermano cuyos subgrupos solo aparecen en actividades de una sola
 * plaza, y ahí el `POST` sí hace algo —crea los espejos sin cablearlos, que es el
 * comportamiento documentado como intencionado para el `-Completo`—.
 *
 * <p><b>EL 400 DEL `GET` SE PRESENTA COMO «ya tiene subgrupos», Y ESO ES UNA INFERENCIA POR
 * ELIMINACIÓN, NO UNA LECTURA.</b> El mensaje del servidor NO llega al cliente: la clave de
 * Boot 3 que lo incluía está muerta desde la migración a 4 (`D-F8.6-ii-a`), así que las
 * SIETE guardas que producen un 400 en la ruta del plan llegan aquí indistinguibles. El
 * recuento, para quien añada la octava:
 *
 * <ol>
 *   <li>hermano vacío — INALCANZABLE: no se pide el plan sin hermano elegido.</li>
 *   <li>hermano == grupo — INALCANZABLE: el filtro `id !== grupo.id`.</li>
 *   <li>hermano de otro nivel — INALCANZABLE: el filtro `nivel === grupo.nivel`.</li>
 *   <li>grupo no ORDINARIO — INALCANZABLE: el botón vive dentro del `@if (esOrdinario)`
 *       de la lista.</li>
 *   <li>el grupo YA TIENE subgrupos — ALCANZABLE, y es el segundo intento de replicar:
 *       la salida es deshacer.</li>
 *   <li>un subgrupo del hermano no empieza por `{hermano}-` — ALCANZABLE.</li>
 *   <li>un código de espejo derivado ya existe — ALCANZABLE.</li>
 * </ol>
 *
 * <p><b>Quedan TRES alcanzables, no una.</b> Las dos últimas son condiciones del CATÁLOGO
 * —cómo están nombrados los subgrupos— y esta pantalla no puede excluirlas con ningún
 * filtro. Por eso el texto nombra la quinta como causa probable y menciona las otras dos en
 * vez de afirmarla: decir «ya tiene subgrupos» a secas mandaría a deshacer a quien tiene un
 * problema de nombres y lo dejaría dando vueltas. Si algún día el mensaje del servidor
 * vuelve a viajar, {@link #mensaje} lo prefiere solo y esta inferencia deja de usarse sin
 * tocar nada.
 */
@Component({
  selector: 'app-replicacion-dialogo',
  imports: [],
  templateUrl: './replicacion-dialogo.html',
  styleUrl: './replicacion-dialogo.css',
})
export class ReplicacionDialogo implements OnInit {
  private readonly service = inject(ReplicacionService);
  private readonly grupoService = inject(GrupoService);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);

  /** El grupo NUEVO, el que se puebla. Siempre presente: lo pone quien abre el diálogo. */
  protected readonly grupo = inject<Grupo>(DIALOG_DATA);

  /** {@link SIN_PLAZA} para la plantilla: el `value` de «no matricular aquí». */
  protected readonly sinPlaza = SIN_PLAZA;

  protected readonly estado = signal<EstadoReplicacion>('cargando');
  /** Error de consulta o de escritura. Vacío = sin error. */
  protected readonly error = signal('');

  /** Hermanos ofrecibles, ya filtrados. Ver el javadoc de clase. */
  protected readonly candidatos = signal<Grupo[]>([]);
  /** Código del hermano elegido. Vacío = aún no ha elegido. */
  protected readonly hermano = signal('');

  /** El plan del `GET`, o el del `POST` ya materializado cuando el estado es `'hecho'`. */
  protected readonly plan = signal<PlanReplicacion | null>(null);

  /** ¿Está desplegado el detalle de los replicados? Arranca PLEGADO: ver el javadoc. */
  protected readonly desplegado = signal(false);

  /**
   * Lo contestado hasta ahora, por código de espejo. `''` o ausente = sin contestar;
   * {@link SIN_PLAZA} = «no matricular aquí»; cualquier otra cosa es un `plazaId` en texto,
   * que es como vuelve el valor de un `<select>`.
   */
  protected readonly respuestas = signal<Record<string, string>>({});

  /**
   * Las decisiones que el usuario tiene que tomar: un espejo por cada uno que participe en
   * algún bloque de REPARTO, en orden de aparición, y sin repetir.
   *
   * <p>El enlace espejo → bloque sale de `Via.espejos`, que es el campo que S141 añadió al
   * contrato precisamente para esto: `gruposActuales` nombra GRUPOS y `subgruposACrear` es
   * la lista entera sin atar a ningún bloque, así que sin `espejos` este conjunto no se
   * puede calcular y el `POST` no se puede componer.
   *
   * <p><b>Las opciones de un espejo son las vías de TODOS los bloques donde aparece, no las
   * de uno.</b> Un espejo puede participar en varios bloques de reparto y el servidor le
   * acepta UNA sola plaza, tomada de la unión de las vías de todos ellos
   * (`Destino.plazas()` acumula). Con un solo bloque —el caso normal— la unión es
   * exactamente «las vías de su bloque»; con varios, ofrecer solo las de uno escondería
   * destinos legítimos. Por eso van agrupadas por actividad en `<optgroup>`: con dos
   * bloques, dos vías distintas serían las dos «vía 1» y el rótulo no las distinguiría.
   */
  protected readonly cuestiones = computed<Cuestion[]>(() => {
    const reparto = this.plan()?.reparto ?? [];
    const orden: string[] = [];
    for (const bloque of reparto) {
      for (const via of bloque.vias) {
        for (const espejo of via.espejos) {
          if (!orden.includes(espejo)) {
            orden.push(espejo);
          }
        }
      }
    }
    return orden.map((espejo) => ({
      espejo,
      bloques: reparto
        .filter((b) => b.vias.some((v) => v.espejos.includes(espejo)))
        .map((b) => ({
          actividad: b.actividad,
          opciones: b.vias.map((via, i) => ({ via, ordinal: i + 1 })),
        })),
    }));
  });

  /**
   * ¿Están TODAS contestadas? `every` sobre las cuestiones y no `some` ni un conteo: con
   * `some` bastaría una para habilitar «Aceptar», y el `POST` saldría incompleto —el
   * servidor exige exactamente una asignación por espejo de reparto y rechaza el hueco con
   * un 400—. Sin cuestiones devuelve `true`, que es correcto: un plan sin reparto no tiene
   * nada que contestar.
   */
  protected readonly todasContestadas = computed(() => {
    const dadas = this.respuestas();
    return this.cuestiones().every((c) => (dadas[c.espejo] ?? '') !== '');
  });

  /**
   * ¿Este plan no haría absolutamente nada? Ni espejos que crear ni bloques que tocar. Ver
   * el javadoc de clase: es lo único que esta pantalla BLOQUEA, porque el servidor lo
   * acepta con un 201.
   */
  protected readonly nadaQueCopiar = computed(() => {
    const p = this.plan();
    return (
      p !== null
      && p.subgruposACrear.length === 0
      && p.replicados.length === 0
      && p.reparto.length === 0
    );
  });

  /** ¿Se puede pulsar «Aceptar»? Ni con el plan vacío ni con decisiones sin tomar. */
  protected readonly puedeAceptar = computed(
    () => !this.nadaQueCopiar() && this.todasContestadas(),
  );

  /**
   * Pide el catálogo de grupos y deriva de él los hermanos ofrecibles. Es la única
   * petición del montaje. Un fallo lleva a `'error'` y no a `'eligiendo'` con la lista
   * vacía: sin candidatos no hay nada que elegir, y un desplegable vacío se leería como
   * «este grupo no tiene hermanos» en vez de como «no se pudo consultar».
   */
  ngOnInit(): void {
    this.grupoService.listar().subscribe({
      next: (lista) => {
        this.candidatos.set(lista.filter((g) => this.esHermanoPosible(g)));
        this.estado.set('eligiendo');
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar la lista de grupos'));
        this.estado.set('error');
      },
    });
  }

  /** Los TRES filtros, juntos y en un solo sitio. Ver el javadoc de clase. */
  private esHermanoPosible(candidato: Grupo): boolean {
    return (
      candidato.nivel === this.grupo.nivel
      && candidato.tipo === TIPO_ORDINARIO
      && candidato.id !== this.grupo.id
    );
  }

  /** Rótulo de una vía: asignatura, grupos que ya la cursan y su ordinal en el bloque. */
  protected rotulo(opcion: OpcionVia): string {
    const grupos = opcion.via.gruposActuales.length > 0
      ? opcion.via.gruposActuales.join(', ')
      : 'sin grupos';
    // El plazaCodigo NO entra: es un ordinal técnico inestable. Ver replicacion.model.ts.
    return `${opcion.via.asignatura} · ${grupos} · vía ${opcion.ordinal}`;
  }

  /** Anota la respuesta de un espejo. El valor viene del `<select>`, o sea como texto. */
  protected responder(espejo: string, valor: string): void {
    this.respuestas.update((dadas) => ({ ...dadas, [espejo]: valor }));
  }

  /**
   * Pide el plan del hermano elegido. Deja las respuestas anteriores a cero: si alguien
   * cambia de hermano, las decisiones de antes son de otros espejos y colarlas en el cuerpo
   * daría el 400 de «no corresponde a ningún bloque de reparto».
   */
  protected verPlan(): void {
    const codigo = this.hermano();
    if (codigo === '') {
      return;
    }
    this.estado.set('cargando');
    this.error.set('');
    this.respuestas.set({});
    this.desplegado.set(false);
    this.service.plan(this.grupo.id, codigo).subscribe({
      next: (plan) => {
        this.plan.set(plan);
        this.estado.set('decidiendo');
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensajeDelPlan(err, codigo));
        this.estado.set('error');
      },
    });
  }

  /**
   * Ejecuta la replicación. En error vuelve a `'decidiendo'` y PRESENTA: ni el 409 de
   * dependientes ni el 400 de asignaciones se arreglan cerrando el diálogo, y el usuario
   * tiene que poder leerlos con sus decisiones todavía en pantalla.
   */
  protected aceptar(): void {
    if (!this.puedeAceptar()) {
      return;
    }
    this.estado.set('enviando');
    this.error.set('');
    this.service.replicar(this.grupo.id, this.cuerpo()).subscribe({
      next: (plan) => {
        this.plan.set(plan);
        this.estado.set('hecho');
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(
          err, `No se pudo replicar ${this.grupo.codigo} desde ${this.hermano()}`));
        this.estado.set('decidiendo');
      },
    });
  }

  /**
   * EXACTAMENTE una asignación por espejo de reparto, ni omitida ni repetida: se compone
   * recorriendo {@link #cuestiones}, que ya es la lista sin duplicados, y no las respuestas
   * anotadas, que podrían llevar restos de un hermano anterior.
   *
   * <p>`subgrupo` lleva el código del ESPEJO, no el del original del hermano: es la clave
   * por la que el servidor los nombra y la única que sabría reconocer.
   */
  private cuerpo(): ReplicacionRequest {
    const dadas = this.respuestas();
    return {
      hermano: this.hermano(),
      asignaciones: this.cuestiones().map((c) => ({
        subgrupo: c.espejo,
        // «No matricular aquí» viaja como null, que es respuesta legítima del contrato y
        // no un hueco: el espejo se crea igual y se queda sin plazas.
        plaza: dadas[c.espejo] === SIN_PLAZA ? null : Number(dadas[c.espejo]),
      })),
    };
  }

  /**
   * Deshace lo que se acaba de escribir. En éxito cierra con `true` —los subgrupos han
   * cambiado otra vez—; en error PRESENTA y vuelve a `'hecho'`, porque el 409 es
   * informativo y quien lo recibe tiene que poder leerlo y salir por su pie.
   */
  protected deshacer(): void {
    this.estado.set('deshaciendo');
    this.error.set('');
    this.service.deshacer(this.grupo.id).subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(
          err, `No se pudo deshacer la replicación de ${this.grupo.codigo}`));
        this.estado.set('hecho');
      },
    });
  }

  /** Salida tras haber escrito: la lista recarga. */
  protected cerrarTrasEscribir(): void {
    this.ref.close(true);
  }

  /** Salida sin escribir: la lista no recarga. */
  protected cerrar(): void {
    this.ref.close(false);
  }

  /**
   * El error del `GET` del plan. Un 400 no trae texto que lo distinga, así que se degrada a
   * la inferencia por eliminación documentada en el javadoc de clase; cualquier otro status
   * usa el degradado corriente. Si el mensaje del servidor volviera a viajar,
   * {@link #mensaje} lo prefiere y la inferencia no llega a usarse.
   */
  private mensajeDelPlan(err: HttpErrorResponse, codigoHermano: string): string {
    if (err?.status === 400) {
      return this.mensaje(
        err,
        `No se puede replicar ${this.grupo.codigo} desde ${codigoHermano}: lo más probable`
          + ` es que ${this.grupo.codigo} ya tiene subgrupos, y la replicación puebla un`
          + ' grupo recién creado —para volver a empezar hay que deshacerla—. También'
          + ` podría ser que algún subgrupo de ${codigoHermano} no empiece por`
          + ` «${codigoHermano}-», o que un código derivado ya exista`,
      );
    }
    return this.mensaje(
      err, `No se pudo consultar el plan de replicación de ${this.grupo.codigo}`);
  }

  /**
   * Traduce error Http a texto de usuario: mensaje del servidor primero (`message`, luego
   * `error`), degradado con status si no hay. Copiado del patrón de `PdcDialogo` con texto
   * propio; NO extraído a utilidad compartida a propósito: hacerlo tocaría los componentes
   * de H1 (D-F8.6, cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
