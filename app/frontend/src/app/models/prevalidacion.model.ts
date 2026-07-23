/**
 * Modelo TS del contrato de pre-validación (Fase 8, Bloque 8.4-B1). Espejo campo
 * a campo de `AvisoPrevalidacionDTO` de `app.web.dto`.
 *
 * `severidad` es `string`, NO una unión `'ERROR' | 'AVISO'`: el backend serializa
 * `Severidad.name()` (ver `AvisoPrevalidacionDTO.de`), y estrechar aquí obligaría
 * a que un TERCER valor futuro del enum ROMPIERA el parseo en vez de degradar.
 * Mismo criterio que `Violacion.regla`/`Penalizacion.regla` en
 * `diagnostico.model.ts`: string pelado cuando el productor es un `enum.name()`.
 */
export interface AvisoPrevalidacion {
  severidad: string;
  regla: string;
  entidadCodigo: string;
  demanda: number;
  disponible: number;
  descripcion: string;
}
