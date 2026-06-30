package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Tipos de aula compatibles con una asignatura (§4.1).
 *
 * <p>La pseudo-DDL define clave primaria compuesta (asignatura, tipo_aula). Se
 * implementa con id sintético (convención de la capa JPA) + restricción de
 * unicidad sobre (asignatura_id, tipo_aula), que preserva la misma garantía
 * lógica sin clave compuesta.
 */
@Entity
@Table(uniqueConstraints =
        @UniqueConstraint(columnNames = {"asignatura_id", "tipo_aula"}))
public class AsignaturaAulaCompatible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_aula", nullable = false)
    private TipoAula tipoAula;

    protected AsignaturaAulaCompatible() {
        // requerido por JPA
    }

    public AsignaturaAulaCompatible(Asignatura asignatura, TipoAula tipoAula) {
        this.asignatura = asignatura;
        this.tipoAula = tipoAula;
    }

    public Long getId() {
        return id;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public TipoAula getTipoAula() {
        return tipoAula;
    }
}
