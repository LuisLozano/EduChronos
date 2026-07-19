package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

/**
 * Tutoría de un profesor sobre un grupo (§4.1, invariante I4, Bloque 8.5-D2a).
 *
 * <p><b>Por qué clave compuesta y no id sintético.</b> A diferencia de
 * {@link AsignaturaAulaCompatible} —que emula la PK compuesta de la pseudo-DDL con id
 * sintético + UNIQUE—, aquí la pseudo-DDL define (profesor, grupo) como PK y se
 * implementa TAL CUAL con {@link IdClass}: la fila no tiene identidad propia más allá
 * del par, y la PK compuesta da gratis la garantía de "un profesor no aparece dos veces
 * en la misma tutoría" sin una restricción de unicidad aparte.
 *
 * <p><b>Lo que esta entidad NO garantiza.</b> I4 ("cada grupo tiene exactamente un
 * TUTOR_PRINCIPAL") NO es expresable en esta PK: la clave admite N filas del mismo grupo
 * con el mismo rol mientras el profesor difiera. La unicidad del rol principal la valida
 * {@code TutoriaService} sobre la lista entrante ANTES de escribir; a nivel de esquema no
 * hay red que la ataje. Tampoco se exige aquí la mitad "existe al menos uno" de I4: un
 * grupo sin tutoría es un estado alcanzable y válido en la escritura.
 *
 * <p>Sin setters ni mutación nombrada: una tutoría se crea o se borra, nunca se edita
 * (el {@code PUT} del sub-recurso es un reemplazo total, no una edición fila a fila).
 */
@Entity
@IdClass(ProfesorTutoria.ProfesorTutoriaId.class)
public class ProfesorTutoria {

    /**
     * Clave compuesta (profesor, grupo). Sus campos se llaman IGUAL que los campos
     * {@code @Id} de la entidad y son del tipo de la PK de cada referenciada (Long),
     * como exige {@link IdClass}.
     */
    public static class ProfesorTutoriaId implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long profesor;
        private Long grupo;

        public ProfesorTutoriaId() {
            // requerido por JPA
        }

        public ProfesorTutoriaId(Long profesor, Long grupo) {
            this.profesor = profesor;
            this.grupo = grupo;
        }

        @Override
        public boolean equals(Object otro) {
            if (this == otro) {
                return true;
            }
            if (!(otro instanceof ProfesorTutoriaId id)) {
                return false;
            }
            return Objects.equals(profesor, id.profesor) && Objects.equals(grupo, id.grupo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(profesor, grupo);
        }
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Profesor profesor;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoAdministrativo grupo;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private RolTutoria rol;

    protected ProfesorTutoria() {
        // requerido por JPA
    }

    public ProfesorTutoria(Profesor profesor, GrupoAdministrativo grupo, RolTutoria rol) {
        this.profesor = profesor;
        this.grupo = grupo;
        this.rol = rol;
    }

    public Profesor getProfesor() {
        return profesor;
    }

    public GrupoAdministrativo getGrupo() {
        return grupo;
    }

    public RolTutoria getRol() {
        return rol;
    }
}
