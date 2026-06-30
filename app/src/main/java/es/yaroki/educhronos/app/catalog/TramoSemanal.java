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
import java.time.LocalTime;

/**
 * Tramo horario semanal (§4.1). {@code esLectivo=false} para el recreo.
 *
 * <p>{@code siguienteInmediato} es una FK autorreferencial nullable: enlaza al
 * tramo que sigue sin pausa (lo usa el solver para bloques obligatorios de
 * varios tramos, invariante S6).
 */
@Entity
public class TramoSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Dia dia;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private boolean esLectivo;

    /** Entero global de ordenación. */
    @Column(nullable = false)
    private int orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siguiente_inmediato_id")
    private TramoSemanal siguienteInmediato;

    protected TramoSemanal() {
        // requerido por JPA
    }

    public TramoSemanal(Dia dia, LocalTime horaInicio, LocalTime horaFin,
                        boolean esLectivo, int orden, TramoSemanal siguienteInmediato) {
        this.dia = dia;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.esLectivo = esLectivo;
        this.orden = orden;
        this.siguienteInmediato = siguienteInmediato;
    }

    public Long getId() {
        return id;
    }

    public Dia getDia() {
        return dia;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public boolean isEsLectivo() {
        return esLectivo;
    }

    public int getOrden() {
        return orden;
    }

    public TramoSemanal getSiguienteInmediato() {
        return siguienteInmediato;
    }
}
