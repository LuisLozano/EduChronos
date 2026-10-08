package es.yaroki.educhronos.app.persistence;

import es.yaroki.educhronos.app.catalog.Profesor;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
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
 * Una guardia ordinaria repartida en un horario generado (S213, C-reparto-guardias): el profesor
 * está de guardia en ese tramo. La escribe la generación junto a las {@link Sesion}, en la misma
 * transacción, y se va con su horario (FK con {@code on delete cascade}). La única impide dos
 * guardias del mismo profesor en el mismo tramo de un horario; el diagnóstico no lo comprueba.
 */
@Entity
@Table(name = "guardia", uniqueConstraints =
        @UniqueConstraint(columnNames = {"horario_id", "profesor_id", "tramo_id"}))
public class Guardia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horario_id", nullable = false)
    private HorarioGenerado horario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Profesor profesor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tramo_id", nullable = false)
    private TramoSemanal tramo;

    protected Guardia() {
        // requerido por JPA
    }

    public Guardia(HorarioGenerado horario, Profesor profesor, TramoSemanal tramo) {
        this.horario = horario;
        this.profesor = profesor;
        this.tramo = tramo;
    }

    public Long getId() {
        return id;
    }

    public HorarioGenerado getHorario() {
        return horario;
    }

    public Profesor getProfesor() {
        return profesor;
    }

    public TramoSemanal getTramo() {
        return tramo;
    }
}
