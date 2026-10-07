/**
 * Espejo del sub-recurso de las aulas de una asignatura, `GET/PUT /api/asignaturas/{id}/aulas`
 * (S206, C-reglas-aulas): `AsignaturaAulaDTO` y `AsignaturaAulaRequest` tienen la misma forma.
 *
 * <p>`aula` es el CÓDIGO del aula, nunca su id. `rol` dice cómo usa la asignatura sus aulas:
 * `EXCLUSIVA` (solo puede ir a ellas) o `PREFERIDA` (mejor en ellas, si se puede). El backend no
 * admite las dos marcas en la misma asignatura: el PUT con mezcla es un 400.
 */
export type RolAulaAsignatura = 'EXCLUSIVA' | 'PREFERIDA';

export interface AsignaturaAula {
  aula: string;
  rol: RolAulaAsignatura;
}
