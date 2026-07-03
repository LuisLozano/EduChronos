package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Unidad de planificación que el solver coloca en el tiempo (§4.6 del modelo).
 * Una actividad agrupa 1..N {@link Plaza}s que ocurren simultáneamente y se
 * repite {@code repeticionesPorSemana} veces a la semana.
 *
 * <p>NO tiene campo {@code tipo}: la naturaleza estructural (ordinaria,
 * co-docencia, desdoble, agrupamiento, bloque de optativas) es inferible del
 * contenido (nº de plazas, profesores por plaza, subgrupos cubiertos). Decisión
 * permanente del plan.
 *
 * <p>{@code asignatura} es opcional (nullable): es null cuando las plazas tienen
 * distintas asignaturas (p. ej. bloque CyR/OyD/RefMt). En ese caso cada
 * {@link Plaza} porta su propia asignatura, que sí es obligatoria.
 *
 * <p>NO existe una entidad {@code ActividadInstancia}: las repeticiones son un
 * artefacto derivado que el dominio del solver expande en runtime
 * ({@code cpsat.Expansion}) a partir de {@code repeticionesPorSemana}. Decisión
 * D-B5-1: no se materializa como tabla. Ver nota en §4.6 del modelo.
 */
@Entity
@Table(name = "actividad")
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    // Opcional: null si las plazas tienen distintas asignaturas (§4.6).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignatura_id")            // nullable por defecto
    private Asignatura asignatura;

    @Column(name = "duracion_tramos", nullable = false)
    private int duracionTramos = 1;

    @Column(name = "repeticiones_por_semana", nullable = false)
    private int repeticionesPorSemana = 1;

    @Column(name = "patron_temporal", nullable = false)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private PatronTemporal patronTemporal = PatronTemporal.NEUTRA;

    @Column(name = "requiere_tutor", nullable = false)
    private boolean requiereTutor = false;

    // Plaza es dependiente de Actividad: cascade ALL + orphanRemoval.
    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Plaza> plazas = new ArrayList<>();

    protected Actividad() { }   // JPA

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public void setAsignatura(Asignatura asignatura) {
        this.asignatura = asignatura;
    }

    public int getDuracionTramos() {
        return duracionTramos;
    }

    public void setDuracionTramos(int duracionTramos) {
        this.duracionTramos = duracionTramos;
    }

    public int getRepeticionesPorSemana() {
        return repeticionesPorSemana;
    }

    public void setRepeticionesPorSemana(int repeticionesPorSemana) {
        this.repeticionesPorSemana = repeticionesPorSemana;
    }

    public PatronTemporal getPatronTemporal() {
        return patronTemporal;
    }

    public void setPatronTemporal(PatronTemporal patronTemporal) {
        this.patronTemporal = patronTemporal;
    }

    public boolean isRequiereTutor() {
        return requiereTutor;
    }

    public void setRequiereTutor(boolean requiereTutor) {
        this.requiereTutor = requiereTutor;
    }

    public List<Plaza> getPlazas() {
        return plazas;
    }

    public void setPlazas(List<Plaza> plazas) {
        this.plazas = plazas;
    }
}