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
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Un aula de una asignatura, exclusiva o preferida (S206, C-reglas-aulas, esquema 4).
 *
 * <p>Clave compuesta (asignatura, aula) con {@link IdClass}, como {@link ProfesorTutoria}: la fila
 * no tiene identidad propia más allá del par, y la PK da gratis que un aula no aparezca dos veces
 * en la misma asignatura. Que una asignatura no mezcle {@link RolAulaAsignatura#EXCLUSIVA} y
 * {@link RolAulaAsignatura#PREFERIDA} no cabe en la clave: lo valida {@code AsignaturaService}.
 *
 * <p>La FK a {@code asignatura} es {@code on delete cascade} (las aulas son población propia de
 * la asignatura, como sus compatibilidades); la de {@code aula} no, y el borrado del aula da 409.
 *
 * <p>Sin setters: una fila se crea o se borra; el {@code PUT} del sub-recurso es un reemplazo total.
 */
@Entity
@Table(name = "asignatura_aula")
@IdClass(AsignaturaAula.AsignaturaAulaId.class)
public class AsignaturaAula {

    /** Clave compuesta (asignatura, aula), con los nombres de los campos {@code @Id}. */
    public static class AsignaturaAulaId implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long asignatura;
        private Long aula;

        public AsignaturaAulaId() {
            // requerido por JPA
        }

        public AsignaturaAulaId(Long asignatura, Long aula) {
            this.asignatura = asignatura;
            this.aula = aula;
        }

        @Override
        public boolean equals(Object otro) {
            if (this == otro) {
                return true;
            }
            if (!(otro instanceof AsignaturaAulaId id)) {
                return false;
            }
            return Objects.equals(asignatura, id.asignatura) && Objects.equals(aula, id.aula);
        }

        @Override
        public int hashCode() {
            return Objects.hash(asignatura, aula);
        }
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id", nullable = false)
    private Aula aula;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private RolAulaAsignatura rol;

    protected AsignaturaAula() {
        // requerido por JPA
    }

    public AsignaturaAula(Asignatura asignatura, Aula aula, RolAulaAsignatura rol) {
        this.asignatura = asignatura;
        this.aula = aula;
        this.rol = rol;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public Aula getAula() {
        return aula;
    }

    public RolAulaAsignatura getRol() {
        return rol;
    }
}
