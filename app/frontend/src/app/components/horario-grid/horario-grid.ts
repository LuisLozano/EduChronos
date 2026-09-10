import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import {
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  afterRenderEffect,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';

import { SesionVista } from '../../models/horario.model';
import { DIAS, InstanciaCelda, TRAMOS, agruparPorActividad, claveSlot } from '../../horario/proyeccion';
import { clavePin } from '../../horario/pines';
import { altoDeCelda } from '../../horario/reparto';
import { ocultasEnCelda } from '../../horario/oculto';
import { ViolacionEnCelda } from '../../horario/diagnostico';

/**
 * Petición de AJUSTE nacida de una suelta: la instancia arrastrada, el tramo
 * destino en coordenadas de `TramoRef`, y quién ocupaba ya ese tramo.
 *
 * <p>Lleva los `ocupantes` porque el contenedor tiene que elegir ENTRE DOS
 * ENDPOINTS —mover o intercambiar— y esa elección depende de cuántas instancias
 * hay en el destino, que solo esta capa sabe: es la rejilla la que agrupa la
 * proyección por slot. Pasarlas ya agrupadas evita que el contenedor rehaga el
 * agrupamiento sobre las sesiones filtradas.
 *
 * <p>Contarlas NO es validar. La rejilla no dice si el movimiento es legal —eso lo
 * dice el servidor—; dice cuántas clases se ven en ese tramo, que es un hecho de lo
 * que hay pintado.
 */
export interface AjusteInstancia {
  actividadCodigo: string;
  indice: number;
  dia: number;
  orden: number;
  /**
   * Instancias que YA hay en el slot destino, tal como
   * {@link agruparPorActividad} las tiene agrupadas. Vacío si el destino está
   * libre. NUNCA incluye a la arrastrada: la emisión se corta antes cuando el
   * destino es el origen.
   *
   * <p>CIEGO a lo que la vista no muestra, por la misma razón que
   * {@link HorarioGrid#slotsOcupados}: en la vista por grupo no se ven las clases
   * de otros grupos, así que un destino que aquí sale vacío puede tener ocupantes
   * invisibles. Eso no rompe nada —el veredicto sigue siendo del servidor, que ve
   * el horario entero—, pero sí explica por qué a veces se manda `mover` donde un
   * humano habría esperado `intercambiar`.
   */
  ocupantes: readonly InstanciaCelda[];
}

/**
 * Rejilla reutilizable de 5 días × 6 tramos. Recibe una lista de `SesionVista`
 * YA filtrada (por grupo, profesor o aula) y la agrupa por `(dia, tramo)` y, ya
 * dentro del slot, por instancia. Cada sub-entrada muestra asignatura, aula,
 * profesores (lista) y una marca con los grupos: no asume cardinalidad 1 en
 * profesores ni en grupos.
 *
 * <p>Dos modos de pintado, por número de plazas de la instancia (D4): una plaza
 * se pinta en DOS líneas; varias plazas se pintan como UN bloque —un rótulo
 * común y una línea por plaza—. La diferencia es de disposición, no de
 * contenido: el DOM es el mismo y el modificador `--fila` gira el eje.
 *
 * <p>La unidad arrastrable es la INSTANCIA, nunca la sub-entrada (D-F8.6-A-2):
 * las 6 plazas de un bloque comparten tramo y se mueven juntas. La rejilla NO
 * mueve nada al soltar —sigue pintando la proyección vigente del servidor, que
 * no cambia hasta regenerar— ni habla con el servicio: emite `soltar` y
 * `despinar`, y el contenedor decide qué hacer con ambos.
 *
 * <p>Los dos outputs hablan el vocabulario de la INSTANCIA, no el de la
 * persistencia: `despinar` emite la CLAVE de {@link clavePin}, nunca el `id` del
 * bloqueo. La rejilla ignora que los pines tengan id, y por eso también ignora
 * que ese id pueda faltar: el candado es SIEMPRE un botón, y resolver la clave
 * —o descubrir que no se puede— es trabajo del contenedor.
 */
@Component({
  selector: 'app-horario-grid',
  imports: [DragDropModule],
  templateUrl: './horario-grid.html',
  styleUrl: './horario-grid.css',
})
export class HorarioGrid {
  readonly sesiones = input.required<readonly SesionVista[]>();
  /**
   * Instancias ya pinadas, indexadas por la clave de {@link clavePin}: pintan
   * candado. El valor del mapa —el id del bloqueo— NO se lee aquí; se acepta en
   * el tipo para no obligar al contenedor a construir una proyección aparte.
   */
  readonly pinadas = input<ReadonlyMap<string, number | null>>(new Map<string, number | null>());
  /**
   * Suma con signo del coste blando de cada instancia (clave de {@link clavePin}),
   * YA agregada por el contenedor: la rejilla PINTA el número, no lo calcula ni
   * conoce las penalizaciones que lo componen. No es `Totales` y no debe
   * "cuadrarse" con nada. Una clave ausente significa "sin badge" —las de suma 0
   * no llegan (C2/S65)—, así que el predicado es `has`, sin comparar con 0.
   */
  readonly badges = input<ReadonlyMap<string, number>>(new Map<string, number>());
  /**
   * Violaciones DURAS por instancia (clave de {@link clavePin}), cada una vista
   * desde una de sus celdas ({@link ViolacionEnCelda}). La rejilla PINTA el
   * resalte; no calcula el índice —lo recibe ya construido por la capa pura—.
   *
   * <p>La asimetría D15 se resuelve AQUÍ, al pintar, y no antes: solo esta capa
   * enumera las sub-entradas y conoce su `plazaCodigo`, así que solo aquí se puede
   * decidir si una violación de aula (plaza no-null) casa con ESTA sub-entrada o
   * si una violación de instancia (plaza null) tiñe la celda entera. Por eso el
   * input es UN mapa sin partir, no dos ya separados por granularidad.
   */
  readonly violaciones = input<ReadonlyMap<string, readonly ViolacionEnCelda[]>>(
    new Map<string, readonly ViolacionEnCelda[]>(),
  );

  /**
   * Código del grupo que la vista está mostrando, o `null` en las vistas de
   * profesor y de aula, donde no hay ninguno implícito. Gobierna la marca de
   * grupos (D6): con grupo actual, la lista se condensa porque repite lo que ya
   * estás mirando —medido: en 23 de 51 sub-entradas de 1B-A `grupos` vale
   * exactamente el grupo de la vista—; sin él, la lista se pinta entera, porque
   * ahí sí informa y condensarla sería retirar una señal existente.
   *
   * <p>La densidad sólo se juega en la vista por grupo (A5): las otras dos tienen
   * una sub-entrada por celda y no necesitan la altura que esto ahorra.
   */
  readonly grupoActual = input<string | null>(null);

  /**
   * Tras qué tramo lectivo va la fila de recreo (D7), o `null` para no pintarla.
   * La rejilla recibe una POSICIÓN ya derivada y no sabe qué es una jornada, igual
   * que no sabe qué es un bloqueo: quien la calcula es {@link recreoTrasTramo}.
   *
   * <p>El defecto `null` importa: sin jornada cargada la rejilla NO inventa un
   * recreo. Una fila de recreo en el sitio equivocado es peor que ninguna.
   */
  readonly recreoTras = input<number | null>(null);

  readonly soltar = output<AjusteInstancia>();
  /** Petición de quitar el pin de una instancia, por CLAVE de {@link clavePin}. */
  readonly despinar = output<string>();
  /**
   * Petición de PONER un pin, por CLAVE de {@link clavePin}. Simétrico exacto de
   * {@link despinar}: la rejilla emite la clave y el contenedor decide qué endpoint
   * toca y con qué cuerpo. Dos outputs y no uno con bandera, porque son dos
   * peticiones distintas —un POST y un DELETE— y el contenedor ya tiene un método
   * por cada una; un `output<{clave, pinar}>` obligaría a desempaquetar allí lo que
   * aquí ya está decidido.
   */
  readonly pinar = output<string>();

  protected readonly dias = DIAS;
  protected readonly tramos = TRAMOS;
  /** Espejo de la función pura para que la plantilla la invoque (como {@link dias}). */
  protected readonly claveSlot = claveSlot;

  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Último tope publicado, para NO reescribir la variable con el mismo valor.
   * Esa guarda es lo que corta el bucle del observador: reescribir dispara una
   * notificación más aunque el número no cambie.
   */
  private altoPublicado: number | null = null;

  /**
   * Espejo en SEÑAL del tope publicado. No es la fuente de verdad —el estilo lo
   * sigue escribiendo `setProperty`— sino el DISPARADOR que le faltaba a D11:
   * `repartirAltura` corre desde el `ResizeObserver`, fuera del ciclo de render,
   * así que fijar `--alto-celda` no agenda ninguna pasada. Sin este espejo, en el
   * primer pintado se mide la celda ANTES de que tenga tope y no se marca nada.
   */
  private readonly altoCelda = signal<number | null>(null);

  /**
   * Plazas ocultas por instancia (clave de {@link clavePin}), medidas sobre el
   * DOM ya pintado. Sólo lleva las instancias con al menos una oculta.
   */
  protected readonly ocultas = signal<ReadonlyMap<string, number>>(new Map<string, number>());

  /** Copia sin señal del último mapa, para comparar sin crearse una dependencia. */
  private ultimasOcultas: ReadonlyMap<string, number> = new Map<string, number>();

  /**
   * Reparto de altura (D1). Se mide el hueco que el flex deja a este componente
   * —que ya lleva descontado todo lo que la vista pinta encima— y se reparte entre
   * los seis tramos, descontando `thead` y la fila de recreo.
   *
   * <p>NO hay bucle: el resultado depende del hueco, del `thead` y del recreo, y de
   * NINGUNA medida de la tabla. Un cambio de la tabla puede disparar el observador,
   * pero recalcula el mismo número, no se reescribe nada y ahí acaba.
   *
   * <p>Se observa el HOST y no un elemento interno porque el host es lo único que la
   * tabla no puede estirar: `flex: 1 1 0` con `min-height: 0` lo ata al hueco
   * disponible. Observar algo que el contenido pueda agrandar sí sería un bucle.
   */
  private repartirAltura(): void {
    const raiz = this.host.nativeElement;
    const thead = raiz.querySelector('thead');
    const recreo = raiz.querySelector('tr.recreo');
    const alto = altoDeCelda(
      raiz.clientHeight,
      thead?.getBoundingClientRect().height ?? 0,
      recreo?.getBoundingClientRect().height ?? 0,
      this.tramos.length,
    );
    if (alto === this.altoPublicado) {
      return;
    }
    this.altoPublicado = alto;
    this.altoCelda.set(alto);
    if (alto === null) {
      raiz.style.removeProperty('--alto-celda');
    } else {
      raiz.style.setProperty('--alto-celda', `${alto}px`);
    }
  }

  /**
   * Mide qué plazas esconde el recorte (D11) y publica el resultado.
   *
   * <p>Va en la fase `read` de {@link afterRenderEffect} y no en el
   * `ResizeObserver`: ese observador sólo despierta con cambios de TAMAÑO, y
   * cambiar de grupo repinta la rejilla sin mover un píxel —`table-layout: fixed`
   * y seis filas—, así que las marcas del grupo anterior sobrevivirían.
   *
   * <p>NO hay bucle, y la razón es estructural: la marca vive en la banda
   * `position: absolute` que `.instancia.bloque` ya reserva, luego no cambia
   * ninguna medida. La segunda pasada mide lo mismo, el mapa es igual y la guarda
   * no reescribe.
   *
   * <p>VERIFICADO en el M4 de S127: Firefox maximizado, viewport 1920x887, dpr 1,
   * hueco 716 px, `--alto-celda` 101 px. La consola no dio UN SOLO aviso de
   * «ResizeObserver loop», que es como se habría manifestado la realimentación que
   * el párrafo anterior descarta por construcción.
   *
   * <p>Las marcas se contaron en tres grupos y cuadran con el cálculo sobre el
   * volcado de la BD demo: 1B-A 4, 4ºA 3 —dos `+2` de seis plazas y un `+1` de
   * cinco— y 2B-B 0 sobre seis celdas de CUATRO plazas. El caso que decide es 4ºA:
   * en la misma pantalla conviven las tres marcadas y tres celdas de cuatro plazas
   * SIN marcar, así que la regla de la mitad discrimina justo donde se juega —esas
   * cuatro plazas se pasan 1,23 px del hueco y se ven al 94 %—. Al cambiar de grupo
   * y volver, las marcas siguen al grupo pintado: es lo que compra estar en la fase
   * `read` y no en el `ResizeObserver`.
   *
   * <p>Y la prueba de que D1 compró algo real, no una constante disfrazada: con el
   * aviso «1 pines sin aplicar» en pantalla el hueco encoge, y la celda de seis
   * plazas pasa a `+3` —medido en 1ºA— SIN que reaparezca el scroll. Con un reparto
   * de constantes, esos 62 px de aviso habrían devuelto la barra.
   *
   * <p>La cadena completa —medir, poblar el mapa, pintar la marca— SÍ se prueba en
   * jsdom, con `getBoundingClientRect` stubeado a la geometría de S126: casos (28)
   * y (29) del spec. Lo que jsdom no puede dar son los rectángulos de verdad —los
   * suyos son ceros—, y ÉSO es lo que aporta M4: que el navegador real produzca la
   * geometría que el stub supone.
   *
   * <p>El mapa recorre TODAS las instancias, incluidas las de una plaza, pero la
   * marca sólo se pinta dentro del `@if (esBloque(inst))`. Las claves de instancias
   * de una plaza entran en el mapa y no las lee nadie: es inocuo y coherente con la
   * limitación de abajo, pero el mapa NO es una proyección fiel de lo que se ve.
   *
   * <p>LIMITACIÓN DECLARADA: sólo se marca lo que tiene banda —modo bloque o
   * badge—. Una instancia de UNA plaza no la tiene, y dársela cambiaría su altura,
   * que es justo la realimentación que esto evita. Hoy no se recorta ninguna (62,9
   * px contra ~110 disponibles), pero es propiedad de estos datos, no del modelo.
   */
  private medirOcultas(): void {
    // `inject(ElementRef)` devuelve `ElementRef<any>` —el `<HTMLElement>` de la
    // declaración es el token, no el genérico—, y sobre `any` la inferencia de
    // `Array.from` cae a `unknown`. Se ancla el tipo aquí, una vez.
    const raiz: HTMLElement = this.host.nativeElement;
    const nuevas = new Map<string, number>();
    for (const celda of raiz.querySelectorAll<HTMLElement>('.celda')) {
      const rectCelda = celda.getBoundingClientRect();
      for (const instancia of celda.querySelectorAll<HTMLElement>('.instancia')) {
        const clave = instancia.getAttribute('data-clave');
        if (clave === null) {
          continue;
        }
        const plazas = Array.from(
          instancia.querySelectorAll<HTMLElement>('.entrada'),
          (e: HTMLElement) => e.getBoundingClientRect(),
        );
        const n = ocultasEnCelda(rectCelda, plazas);
        if (n !== null && n > 0) {
          nuevas.set(clave, n);
        }
      }
    }
    const previo = this.ultimasOcultas;
    const igual =
      previo.size === nuevas.size &&
      [...nuevas].every(([k, v]) => previo.get(k) === v);
    if (igual) {
      return;
    }
    this.ultimasOcultas = nuevas;
    this.ocultas.set(nuevas);
  }

  /** Detalle de todas las plazas, para el `title` de la marca de D11. */
  protected detalleInstancia(inst: InstanciaCelda): string {
    return inst.entradas.map((e) => `${e.asignaturaCodigo} (${e.aulaCodigo})`).join(', ');
  }

  /** Plazas ocultas de una instancia, o `null` si no esconde ninguna (D11). */
  protected marcaOcultas(inst: InstanciaCelda): number | null {
    return this.ocultas().get(this.clave(inst)) ?? null;
  }

  constructor() {
    afterRenderEffect({
      read: () => {
        // Dependencias EXPLÍCITAS: el contenido y el tope. Las lee aquí y no
        // dentro de medirOcultas para que el disparo no dependa de por dónde
        // pase el recorrido del DOM.
        this.celdas();
        this.altoCelda();
        this.medirOcultas();
      },
    });

    afterNextRender(() => {
      const observador = new ResizeObserver(() => this.repartirAltura());
      // El host, para el hueco; la tabla, porque la fila de recreo aparece DESPUÉS
      // (la jornada llega por HTTP) y cambia el reparto sin cambiar el hueco.
      observador.observe(this.host.nativeElement);
      const tabla = this.host.nativeElement.querySelector('table');
      if (tabla) {
        observador.observe(tabla);
      }
      this.destroyRef.onDestroy(() => observador.disconnect());
    });
  }

  private readonly celdas = computed(() => agruparPorActividad(this.sesiones()));

  /**
   * Clave de {@link clavePin} de la instancia que se está arrastrando; `null` en
   * reposo. La fija {@link alIniciarArrastre} y la limpia {@link alTerminarArrastre};
   * es la entrada de {@link slotsOcupados}.
   */
  private readonly arrastrando = signal<string | null>(null);

  /**
   * Claves de {@link claveSlot} de los slots que, DURANTE un arrastre, ya tienen
   * al menos una instancia distinta de la que se arrastra. En reposo
   * (`arrastrando() === null`) es el conjunto vacío: nada se marca. Cuenta
   * {@link InstanciaCelda}, no `entradas`, así que un desdoble de seis plazas es
   * UNA instancia y ocupa su slot una sola vez; la clave del propio arrastrado se
   * excluye para no teñir su slot de origen.
   *
   * <p>(a) Afirma solo «en este tramo YA HAY clase EN LA VISTA ACTUAL». NO afirma
   * que soltar ahí viole ninguna restricción: es una ayuda visual, no un veredicto.
   *
   * <p>(b) Es CIEGO por construcción a los recursos que la vista no muestra: en la
   * vista por grupo no ve profesor ni aula, en la de aula no ve grupo ni profesor,
   * etc. Un slot sin marca puede seguir siendo inviable por un recurso oculto.
   *
   * <p>(c) NO es una verificación y NO debe crecer hacia una. Portar aquí la lógica
   * de solapes sería un CUARTO espejo de las restricciones —en otro lenguaje y sin
   * el test que protege D15—: la validación es del backend, y esta marca se queda
   * en «hay clase», deliberadamente por debajo de eso.
   */
  protected readonly slotsOcupados = computed<Set<string>>(() => {
    const arrastrando = this.arrastrando();
    if (arrastrando === null) {
      return new Set<string>();
    }
    const ocupados = new Set<string>();
    for (const [slot, instancias] of this.celdas()) {
      if (instancias.some((inst) => this.clave(inst) !== arrastrando)) {
        ocupados.add(slot);
      }
    }
    return ocupados;
  });

  /** Instancias del slot (dia, tramo); vacío si no hay ninguna. */
  protected instancias(dia: number, tramo: number): InstanciaCelda[] {
    return this.celdas().get(claveSlot(dia, tramo)) ?? [];
  }

  /** Registra la instancia que empieza a arrastrarse: abre la marca de ocupación. */
  protected alIniciarArrastre(inst: InstanciaCelda): void {
    this.arrastrando.set(this.clave(inst));
  }

  /** Cierra el arrastre: la marca de ocupación vuelve a reposo (Set vacío). */
  protected alTerminarArrastre(): void {
    this.arrastrando.set(null);
  }

  protected clave(inst: InstanciaCelda): string {
    return clavePin(inst.actividadCodigo, inst.indice);
  }

  protected estaPinada(inst: InstanciaCelda): boolean {
    return this.pinadas().has(this.clave(inst));
  }

  /** Hay badge si la instancia tiene clave en el mapa. Suma 0 no llega (C2/S65). */
  protected tieneBadge(inst: InstanciaCelda): boolean {
    return this.badges().has(this.clave(inst));
  }

  /** El número del badge; `undefined` si no hay, pero solo se lee tras {@link tieneBadge}. */
  protected badge(inst: InstanciaCelda): number | undefined {
    return this.badges().get(this.clave(inst));
  }

  /**
   * Resalte de la INSTANCIA entera: hay alguna violación de esta instancia con
   * `plazaCodigo === null` —las de profesor y subgrupo, que no hablan de una
   * plaza concreta (D15)—. `.some` porque una instancia puede tener a la vez
   * violaciones de instancia y de aula, y basta una de las primeras.
   */
  protected tieneViolacionInstancia(inst: InstanciaCelda): boolean {
    return (this.violaciones().get(this.clave(inst)) ?? []).some((v) => v.plazaCodigo === null);
  }

  /**
   * Resalte de UNA sub-entrada: hay alguna violación de aula de esta instancia
   * cuya plaza sea la de ESTA sub-entrada (D15). Se compara `plazaCodigo`, nunca
   * `aulaCodigo`: dos plazas distintas pueden compartir aula y la clave de la
   * violación es la plaza. `.some` evalúa cada entrada por separado, así que en un
   * desdoble (una violación con dos celdas, una por plaza) cada sub-entrada casa
   * con la SUYA (ver T5).
   */
  protected tieneViolacionAula(inst: InstanciaCelda, e: SesionVista): boolean {
    return (this.violaciones().get(this.clave(inst)) ?? []).some((v) => v.plazaCodigo === e.plazaCodigo);
  }

  /**
   * Una instancia se pinta en modo bloque cuando tiene MÁS DE UNA plaza en el
   * mismo tramo (D4): un bloque es una instancia con N plazas, no N clases
   * sueltas, y ésa es también la razón de que sea la unidad arrastrable. El
   * umbral se mide sobre `entradas`, que es lo que la celda pinta, y no sobre la
   * actividad: dos repeticiones de la misma actividad en tramos distintos son dos
   * instancias de una plaza, no un bloque de dos.
   */
  protected esBloque(inst: InstanciaCelda): boolean {
    return inst.entradas.length > 1;
  }

  /**
   * Texto de la marca de grupos (D6), o `null` si no hay nada que decir. Con
   * {@link grupoActual} fijado se cuentan los OTROS grupos —los que comparten la
   * plaza— y el detalle completo queda en el `title`: la cuarta línea se condensa,
   * no se elimina. Sin grupo actual se devuelve la lista tal cual.
   *
   * <p>Devuelve `null` y no cadena vacía porque la plantilla decide con ello si
   * pinta el elemento: una marca vacía seguiría ocupando su sitio en la línea.
   */
  protected marcaGrupos(e: SesionVista): string | null {
    const actual = this.grupoActual();
    if (actual === null) {
      return e.grupos.length === 0 ? null : e.grupos.join(', ');
    }
    const otros = e.grupos.filter((g) => g !== actual).length;
    return otros === 0 ? null : `+${otros}`;
  }

  /**
   * ALTERNA el pin de la instancia: emite `pinar` si no lo tiene y `despinar` si lo
   * tiene. El sentido se decide aquí y no en el contenedor porque la rejilla ya sabe
   * cuál es el estado —lo está pintando en el glifo—, y hacer que el contenedor lo
   * dedujera otra vez del mapa `pinadas` sería derivar dos veces el mismo hecho, con
   * el riesgo de que las dos derivaciones discrepen.
   *
   * <p>Se emite la CLAVE en los dos sentidos, nunca el id del bloqueo: la rejilla
   * ignora que los pines tengan id, y resolver clave→id —o descubrir que no se
   * puede— es trabajo del contenedor.
   *
   * <p>El `stopPropagation` del CLICK es DEFENSIVO y sigue haciendo falta pese al
   * `(pointerdown)` de la plantilla: hoy ningún ancestro escucha `click`, pero el
   * botón vive dentro del `cdkDrag`, y dejar que el evento suba invitaría a que un
   * listener futuro en la instancia o en la celda tratase el gesto del pin como una
   * selección. Son eventos DISTINTOS y protegen de cosas distintas: el de
   * `pointerdown` impide que el CDK arranque un arrastre desde el candado, cosa que
   * este no puede hacer porque para cuando hay `click` el arrastre ya habría
   * empezado.
   */
  protected alAlternarPin(inst: InstanciaCelda, evento: Event): void {
    evento.stopPropagation();
    const clave = this.clave(inst);
    if (this.estaPinada(inst)) {
      this.despinar.emit(clave);
      return;
    }
    this.pinar.emit(clave);
  }

  /**
   * Traduce el drop del CDK a `soltar`, que desde S145 es una petición de AJUSTE y
   * ya no un alta de pin. Soltar en el mismo slot no es un movimiento y no emite
   * nada; el resto se emite tal cual, SIN VALIDAR: qué endpoint toca y si el
   * movimiento es legal lo deciden el contenedor y el servidor, en ese orden.
   *
   * <p>Los `ocupantes` salen de {@link instancias}, la misma consulta que pinta la
   * celda: el destino se lee de lo que HAY, no de lo que se supone. Se leen ANTES
   * de emitir y con la proyección aún sin tocar, que es la única foto válida —la
   * rejilla no se mueve hasta el 200, así que aquí no hay estado intermedio que
   * pudiera falsear la cuenta—.
   *
   * <p>AVISO: `evento.item.data as InstanciaCelda` es un cast SIN comprobación.
   * Solo es válido mientras el `cdkDropListGroup` de esta plantilla conecte
   * únicamente celdas de esta rejilla, todas con `[cdkDragData]` de ese tipo. Si
   * algún día se conecta otra fuente de arrastre (una paleta lateral, otra
   * rejilla), el cast pasa a ser mentira y falla en runtime sin que el
   * compilador avise: entonces habrá que discriminar el tipo del `data`.
   */
  protected alSoltar(evento: CdkDragDrop<{ dia: number; orden: number }>, dia: number, orden: number): void {
    const inst = evento.item.data as InstanciaCelda;
    const origen = inst.entradas[0];
    if (origen.dia === dia && origen.tramo === orden) {
      return;
    }
    this.soltar.emit({
      actividadCodigo: inst.actividadCodigo,
      indice: inst.indice,
      dia,
      orden,
      ocupantes: this.instancias(dia, orden),
    });
  }
}
