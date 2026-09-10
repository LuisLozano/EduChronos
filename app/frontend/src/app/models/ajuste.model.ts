/**
 * Modelos TS del contrato de AJUSTE MANUAL de instancias (S143 mover, S144
 * intercambiar). Reflejan campo a campo los records de `app.web.dto`:
 * `MoverInstanciaRequest`, `ReferenciaInstancia`, `IntercambiarInstanciasRequest`,
 * `IntercambioRealizadoDTO` y `FalloMovimientoDTO`.
 *
 * <p>Los tipos de la VIOLACIÓN no se redeclaran aquí: el backend reutiliza
 * literalmente `ViolacionDTO`/`CeldaRefDTO` del diagnóstico (8.3-C) para el cuerpo
 * del rechazo, así que el cliente reutiliza {@link Violacion} de
 * `diagnostico.model.ts`. Un segundo espejo de la misma forma se desincronizaría
 * en silencio del primero.
 *
 * <p>Las FILAS que devuelve un 200 son `SesionVista` de `horario.model.ts`, el
 * mismo tipo de las `sesiones` de la proyección: el Javadoc de
 * `IntercambioRealizadoDTO` lo dice explícitamente
 * (D-proyeccion-instancia-espejo). Tampoco eso se duplica.
 */

import { Violacion } from './diagnostico.model';
import { SesionVista } from './horario.model';

/**
 * Espejo de `MoverInstanciaRequest`. Cuerpo del
 * `PUT /api/horarios/{horarioId}/instancias`.
 *
 * <p>El destino va por el par natural (`dia` 1..5, `orden` = ordenEnDia 1..6,
 * recreos excluidos) que ve la rejilla, nunca por id de tramo. NO lleva aula: el
 * movimiento CONSERVA la de cada fila.
 */
export interface MoverInstanciaRequest {
  actividadCodigo: string;
  indice: number;
  dia: number;
  orden: number;
}

/**
 * Espejo de `ReferenciaInstancia`. Una instancia por su clave de negocio, el par
 * (`actividadCodigo`, `indice`) —D-6—, nunca por `sesionId`: una instancia son de
 * 1 a 6 filas de sesión y se mueven todas o ninguna.
 */
export interface ReferenciaInstancia {
  actividadCodigo: string;
  indice: number;
}

/**
 * Espejo de `IntercambiarInstanciasRequest`. Cuerpo del
 * `PUT /api/horarios/{horarioId}/instancias/intercambio`.
 *
 * <p>`primera`/`segunda` son deliberadamente simétricos y NO «origen»/«destino»:
 * la operación es simétrica. El orden sí decide UNA cosa —a cuál de las dos nombra
 * el servidor cuando el rechazo habla de un lado—, y por eso los mensajes de fallo
 * dicen siempre de qué lado hablan.
 *
 * <p>NO lleva tramo: el destino de cada una es donde está la otra.
 */
export interface IntercambiarInstanciasRequest {
  primera: ReferenciaInstancia;
  segunda: ReferenciaInstancia;
}

/**
 * Espejo de `IntercambioRealizadoDTO`. Cuerpo del 200 del intercambio: DOS listas,
 * una por lado, NUNCA una concatenada.
 *
 * <p>Se tipa literal por la razón que el Javadoc del record da: quien llamó pidió
 * por dos referencias y tiene que poder mirar cada lado por separado SIN reagrupar
 * por `actividadCodigo`. Aplanarlas aquí en una sola lista tiraría exactamente la
 * información que el backend se molesta en preservar —y las dos instancias pueden
 * ser de la MISMA actividad con índices distintos, así que la reagrupación no sería
 * ni siquiera reversible—.
 */
export interface IntercambioRealizado {
  /** Filas de la instancia `primera`, ya en el tramo que ocupaba `segunda`. */
  primera: SesionVista[];
  /** Ídem, en el tramo que ocupaba `primera`. */
  segunda: SesionVista[];
}

/**
 * Espejo de `FalloMovimientoDTO`. Cuerpo de todo no-2xx de los dos endpoints.
 *
 * <p>Lo construye el backend a mano (no `ResponseStatusException`) precisamente
 * para que `violaciones` viaje por la red: el rechazo por regla dura no es un
 * texto, es una lista que la vista tiene que pintar.
 */
export interface FalloMovimiento {
  /**
   * Símbolo estable del hecho: `TRAMO_INEXISTENTE`, `HORARIO_INEXISTENTE`,
   * `INSTANCIA_INEXISTENTE`, `VIOLA_REGLA_DURA`, `INSTANCIA_PINADA`,
   * `INSTANCIAS_IGUALES`. Es lo que la vista lee para decidir qué decir; NO es
   * texto para el usuario. String pelado, igual que `Violacion.regla`: estrechar a
   * unión de literales afirmaría que el servidor no puede añadir causas.
   */
  causa: string;
  /**
   * Prosa del servidor, para el log y para quien depure. La vista NO decide con
   * ella —decide con {@link causa}—, con UNA excepción documentada: en
   * `INSTANCIA_INEXISTENTE` es el único sitio donde viaja CUÁL de las dos
   * instancias falta (el servidor lo interpola como `La instancia 'primera' (…)`).
   */
  mensaje: string;
  /**
   * Violaciones duras que APARECEN por causa del movimiento y no estaban antes.
   * Lista vacía en toda causa que no sea `VIOLA_REGLA_DURA`; nunca null.
   */
  violaciones: Violacion[];
}
