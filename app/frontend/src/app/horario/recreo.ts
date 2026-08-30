import { TramoJornadaDTO } from '../models/jornada.model';

/**
 * Tras qué tramo LECTIVO va la fila de recreo (D7), o `null` si no hay ninguno.
 *
 * <p>El recreo no es un tramo más de la rejilla: `ordenEnDia` vale null en los no
 * lectivos («un recreo no tiene sitio en la numeración 1..6»), así que no puede
 * entrar en `TRAMOS` sin romper `claveSlot`. Lo que la rejilla necesita es UNA
 * posición, y aquí se DERIVA de la malla en vez de escribirse: si el centro mueve
 * el recreo, la rejilla lo sigue sin tocar nada.
 *
 * <p>Se mira sólo el PRIMER día porque la jornada es idéntica los cinco —el `PUT`
 * recibe un día tipo y el backend lo expande—, y la fila de la rejilla es una
 * sola, común a las cinco columnas.
 *
 * <p>Con varios recreos en un día devuelve el primero: la rejilla pinta UNA fila.
 * Es una limitación consciente del diseño, no un descuido; un centro con dos
 * recreos necesitaría otra cosa, y hoy no existe.
 *
 * <p>Un no lectivo ANTES de la primera clase devuelve `null`: eso no es un recreo,
 * es que la jornada empieza más tarde, y una fila de recreo sobre la primera clase
 * mentiría.
 */
export function recreoTrasTramo(tramos: readonly TramoJornadaDTO[]): number | null {
  if (tramos.length === 0) {
    return null;
  }
  const dia = tramos[0].dia;
  const delDia = tramos.filter((t) => t.dia === dia).sort((a, b) => a.orden - b.orden);
  let lectivos = 0;
  for (const t of delDia) {
    if (t.esLectivo) {
      lectivos += 1;
    } else {
      return lectivos === 0 ? null : lectivos;
    }
  }
  return null;
}
