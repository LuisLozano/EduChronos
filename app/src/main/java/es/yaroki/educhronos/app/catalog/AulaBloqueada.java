package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Pin manual del AULA de una PLAZA concreta de una instancia de actividad antes
 * de resolver (§4.7, Bloque 8.2b-ii). Forma NORMALIZADA que el dominio agrega en
 * el {@code Map<Plaza, Aula> aulasPinadas} del record
 * {@code solver.domain.SesionBloqueada}: una fila por (instancia, plaza), donde
 * cada fila fija una plaza de aula variable a una de sus {@code aulasCandidatas}.
 *
 * <p>El pin de aula es POR PLAZA porque un desdoble tiene varias plazas
 * simultáneas, cada una con su aula. Va SIEMPRE acompañado del pin de tramo de su
 * instancia ({@link SesionBloqueada}): el dominio no soporta un pin de aula suelto.
 * Bloqueo GLOBAL del centro (sin FK a {@code HorarioGenerado}). La restricción
 * única (actividad, indice, plaza) impide dos pines de aula sobre la misma plaza
 * de una misma instancia.
 */
@Entity
@Table(name = "aula_bloqueada", uniqueConstraints =
        @UniqueConstraint(columnNames = {"actividad_id", "indice", "plaza_id"}))
public class AulaBloqueada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @Column(name = "indice", nullable = false)
    private int indice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plaza_id", nullable = false)
    private Plaza plaza;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id", nullable = false)
    private Aula aula;

    protected AulaBloqueada() {
        // requerido por JPA
    }

    public AulaBloqueada(Actividad actividad, int indice, Plaza plaza, Aula aula) {
        this.actividad = actividad;
        this.indice = indice;
        this.plaza = plaza;
        this.aula = aula;
    }

    public Long getId() {
        return id;
    }

    public Actividad getActividad() {
        return actividad;
    }

    public int getIndice() {
        return indice;
    }

    public Plaza getPlaza() {
        return plaza;
    }

    public Aula getAula() {
        return aula;
    }
}
