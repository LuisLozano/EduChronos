package es.yaroki.educhronos.app.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera de un horario persistido: la salida del solver guardada para
 * recuperarse entre sesiones (§4.7, Fase 6 Bloque 9). Agrupa sus {@link Sesion}
 * (una por plaza colocada).
 *
 * <p>{@code estado} (ciclo editorial que decide el usuario) y {@code estadoSolver}
 * (veredicto del solver, {@code CpSolverStatus.name()}) son campos DISTINTOS a
 * propósito. {@code objetivo} y {@code cotaInferior} son {@code Double} NULLABLE
 * reales, no centinela: {@code 0.0} es un objetivo válido y hay que poder
 * distinguirlo de "no medido". La entidad guarda el {@code estadoSolver} como
 * String para no acoplar el esquema al enum de OR-Tools.
 */
@Entity
@Table(name = "horario_generado")
public class HorarioGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "fecha_generacion", nullable = false)
    private Instant fechaGeneracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoHorario estado = EstadoHorario.BORRADOR;

    @Column(name = "estado_solver", nullable = false)
    private String estadoSolver;

    @Column(nullable = true)
    private Double objetivo;

    @Column(name = "cota_inferior", nullable = true)
    private Double cotaInferior;

    @OneToMany(mappedBy = "horario", fetch = FetchType.LAZY)
    private List<Sesion> sesiones = new ArrayList<>();

    protected HorarioGenerado() {
        // requerido por JPA
    }

    public HorarioGenerado(String nombre, Instant fechaGeneracion, String estadoSolver,
                           Double objetivo, Double cotaInferior) {
        this.nombre = nombre;
        this.fechaGeneracion = fechaGeneracion;
        this.estadoSolver = estadoSolver;
        this.objetivo = objetivo;
        this.cotaInferior = cotaInferior;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Instant getFechaGeneracion() {
        return fechaGeneracion;
    }

    public EstadoHorario getEstado() {
        return estado;
    }

    public String getEstadoSolver() {
        return estadoSolver;
    }

    public Double getObjetivo() {
        return objetivo;
    }

    public Double getCotaInferior() {
        return cotaInferior;
    }

    public List<Sesion> getSesiones() {
        return sesiones;
    }
}
