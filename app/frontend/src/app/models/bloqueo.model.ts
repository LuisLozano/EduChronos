/**
 * Modelos TS del contrato de bloqueos manuales (§4.7, Bloque 8.2b-iv). Reflejan
 * campo a campo los records de `app.web.dto`: TramoRefDTO, AulaPinDTO,
 * BloqueoRequest y BloqueoDTO.
 *
 * La clave de cruce con `SesionVista` es el par (`actividadCodigo`, `indice`),
 * que la proyección ya lleva (D-6): por eso no se toca `horario.model.ts`.
 */

/** Espejo de `TramoRefDTO`. El tramo por el par natural que ve la UI. */
export interface TramoRef {
  /** 1..5 (lunes..viernes). */
  dia: number;
  /** ordenEnDia 1..6 (recreos excluidos). Nunca `TramoSemanal.id`. */
  orden: number;
}

/** Espejo de `AulaPinDTO`. Fija UNA plaza de aula variable a una candidata suya. */
export interface AulaPin {
  plazaCodigo: string;
  aulaCodigo: string;
}

/**
 * Espejo de `BloqueoRequest`. Cuerpo del POST: el pin de tramo sobre la
 * instancia (`actividadCodigo`, `indice`) MÁS sus N pines de aula. Describe el
 * estado COMPLETO del pin (D-5, "PUT semántico"): `aulas` vacío deja el pin sin
 * pines de aula, no hay merge parcial.
 */
export interface BloqueoRequest {
  actividadCodigo: string;
  indice: number;
  tramo: TramoRef;
  aulas: AulaPin[];
}

/** Espejo de `BloqueoDTO`. Simétrico a `BloqueoRequest` más el `id` del DELETE. */
export interface Bloqueo {
  id: number;
  actividadCodigo: string;
  indice: number;
  tramo: TramoRef;
  aulas: AulaPin[];
}
