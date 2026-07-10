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
 * Pin manual del TRAMO de una instancia de actividad antes de resolver (§4.7,
 * Bloque 8.2b-ii). Espejo JPA parcial del record
 * {@code solver.domain.SesionBloqueada}: fija el {@code tramoInicio} de la
 * instancia identificada por el par (actividad, {@code indice}). El pin de AULA
 * por plaza —la otra mitad del record de dominio— vive en {@link AulaBloqueada};
 * {@code BloqueoMapper} agrega ambas entidades en el {@code Map<Plaza, Aula>} del
 * record homónimo del solver.
 *
 * <p>Bloqueo GLOBAL del centro: no cuelga de ningún {@code HorarioGenerado} (§4.7,
 * PK lógica = instancia). La instancia NO se materializa como tabla (D-B5-1): se
 * conserva como par (actividad, {@code indice}), igual que {@link Sesion} deriva
 * su instancia de {@code plaza.actividad}. La restricción única (actividad,
 * indice) impide dos pines de tramo contradictorios sobre la misma instancia.
 */
@Entity
@Table(name = "sesion_bloqueada", uniqueConstraints =
        @UniqueConstraint(columnNames = {"actividad_id", "indice"}))
public class SesionBloqueada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @Column(name = "indice", nullable = false)
    private int indice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tramo_inicio_id", nullable = false)
    private TramoSemanal tramoInicio;

    protected SesionBloqueada() {
        // requerido por JPA
    }

    public SesionBloqueada(Actividad actividad, int indice, TramoSemanal tramoInicio) {
        this.actividad = actividad;
        this.indice = indice;
        this.tramoInicio = tramoInicio;
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

    public TramoSemanal getTramoInicio() {
        return tramoInicio;
    }
}
