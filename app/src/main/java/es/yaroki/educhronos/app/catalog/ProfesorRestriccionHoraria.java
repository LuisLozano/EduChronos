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

/**
 * Restricción horaria de un profesor sobre un tramo concreto (§4.3). Espejo JPA
 * del record {@code solver.domain.RestriccionHoraria}, traducido por
 * {@code CatalogoMapper}.
 *
 * <p>Relación dirigida: la restricción apunta a {@code Profesor} y a
 * {@code TramoSemanal}; ninguno de los dos catálogos conoce esta entidad (no se
 * acoplan). {@code peso} solo es semánticamente relevante en {@code BLANDA};
 * {@code motivo} es opcional (texto libre de auditoría).
 */
@Entity
public class ProfesorRestriccionHoraria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Profesor profesor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tramo_id", nullable = false)
    private TramoSemanal tramo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRestriccion tipo;

    @Column(nullable = false)
    private int peso;

    @Column
    private String motivo;

    protected ProfesorRestriccionHoraria() {
        // requerido por JPA
    }

    public ProfesorRestriccionHoraria(
            Profesor profesor, TramoSemanal tramo, TipoRestriccion tipo, int peso, String motivo) {
        this.profesor = profesor;
        this.tramo = tramo;
        this.tipo = tipo;
        this.peso = peso;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Profesor getProfesor() {
        return profesor;
    }

    public TramoSemanal getTramo() {
        return tramo;
    }

    public TipoRestriccion getTipo() {
        return tipo;
    }

    public int getPeso() {
        return peso;
    }

    public String getMotivo() {
        return motivo;
    }
}
