package es.yaroki.educhronos.app.persistence;

import es.yaroki.educhronos.app.catalog.Aula;
import es.yaroki.educhronos.app.catalog.Plaza;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
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
 * Una plaza colocada: la unidad física de salida del solver (§4.7, Fase 6
 * Bloque 9). Una fila por plaza, no por instancia: una {@code ActividadInstancia}
 * con varias plazas (desdoble, agrupamiento, bloque de optativas) produce VARIAS
 * sesiones en el mismo tramo, una por plaza.
 *
 * <p>La identidad lógica de la instancia se conserva como par (plaza.actividad,
 * {@code indice}) sin materializar {@code ActividadInstancia} (D-B5-1). La
 * actividad NO se lleva como FK propia: se deriva por {@code plaza.getActividad()}.
 * {@code aula} guarda el aula EFECTIVAMENTE ocupada (fija de la plaza o la elegida
 * por el solver), resuelta vía {@code SolucionHorario.aulaElegida}; nunca es null.
 */
@Entity
@Table(name = "sesion", uniqueConstraints =
        @UniqueConstraint(columnNames = {"horario_id", "plaza_id", "indice"}))
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horario_id", nullable = false)
    private HorarioGenerado horario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plaza_id", nullable = false)
    private Plaza plaza;

    @Column(name = "indice", nullable = false)
    private int indice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tramo_inicio_id", nullable = false)
    private TramoSemanal tramoInicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id", nullable = false)
    private Aula aula;

    protected Sesion() {
        // requerido por JPA
    }

    public Sesion(HorarioGenerado horario, Plaza plaza, int indice,
                  TramoSemanal tramoInicio, Aula aula) {
        this.horario = horario;
        this.plaza = plaza;
        this.indice = indice;
        this.tramoInicio = tramoInicio;
        this.aula = aula;
    }

    public Long getId() {
        return id;
    }

    public HorarioGenerado getHorario() {
        return horario;
    }

    public Plaza getPlaza() {
        return plaza;
    }

    public int getIndice() {
        return indice;
    }

    public TramoSemanal getTramoInicio() {
        return tramoInicio;
    }

    public Aula getAula() {
        return aula;
    }
}
