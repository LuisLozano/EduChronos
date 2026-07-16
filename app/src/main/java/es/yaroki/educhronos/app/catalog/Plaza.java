package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * Una asignación concreta de asignatura + profesores + aula + subgrupos dentro
 * de una {@link Actividad} (§4.6 del modelo). Entidad dependiente: no existe
 * fuera de su actividad (dueña del ciclo de vida vía cascade/orphanRemoval).
 *
 * <p>{@code asignatura} es obligatoria aunque {@code Actividad.asignatura} sea
 * null (§4.6). El aula sigue el XOR aula_fija / aulasCandidatas: exactamente
 * una de las dos. Ese XOR NO lo valida la entidad JPA (POJO de persistencia);
 * lo hace cumplir el record del dominio al construirse desde el mapper (decisión
 * D-B5-2, coherente con el reparto de validación del plan).
 *
 * <p>Tres relaciones M:N: profesores (I7: al menos uno), subgrupos (población),
 * aulasCandidatas (solo si aula_fija es null).
 */
@Entity
@Table(name = "plaza")
public class Plaza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    // Rama aulaFija del XOR: opcional. Si null, se usan aulasCandidatas.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aula_fija_id")             // nullable
    private Aula aulaFija;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "plaza_profesor",
            joinColumns = @JoinColumn(name = "plaza_id"),
            inverseJoinColumns = @JoinColumn(name = "profesor_id"))
    private Set<Profesor> profesores = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "plaza_aula_candidata",
            joinColumns = @JoinColumn(name = "plaza_id"),
            inverseJoinColumns = @JoinColumn(name = "aula_id"))
    private Set<Aula> aulasCandidatas = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "plaza_subgrupo",
            joinColumns = @JoinColumn(name = "plaza_id"),
            inverseJoinColumns = @JoinColumn(name = "subgrupo_id"))
    private Set<Subgrupo> subgrupos = new HashSet<>();

    protected Plaza() { }   // JPA

    /**
     * Ctor de dependencia (visible en el paquete): fija la {@link Actividad} dueña y el
     * {@code codigo}, ambos estructurales (el código, tras la reevaluación de 8.5-C1, es
     * ESTABLE: no cambia mientras la plaza sobreviva). Lo usa {@link Actividad#agregarPlaza}
     * para que la raíz del agregado construya sus plazas sin exponer este ctor al servicio.
     */
    Plaza(Actividad actividad, String codigo) {
        this.actividad = actividad;
        this.codigo = codigo;
    }

    /**
     * Reasigna el CONTENIDO editable de una plaza (CRUD 8.5-C1): asignatura, aula
     * (fija/candidatas) y las tres M:N. Mutación de dominio nombrada en lugar de setters
     * libres. NO toca el {@code codigo} (estable, se conserva en la reconciliación
     * posicional del PUT), ni el {@code id}, ni la {@link #actividad} dueña. Los tres
     * conjuntos M:N se REEMPLAZAN con copia defensiva, como {@link Subgrupo#actualizar}.
     */
    public void actualizar(Asignatura asignatura, Aula aulaFija,
            Set<Profesor> profesores, Set<Aula> aulasCandidatas, Set<Subgrupo> subgrupos) {
        this.asignatura = asignatura;
        this.aulaFija = aulaFija;
        this.profesores = new HashSet<>(profesores);
        this.aulasCandidatas = new HashSet<>(aulasCandidatas);
        this.subgrupos = new HashSet<>(subgrupos);
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Actividad getActividad() {
        return actividad;
    }

    public void setActividad(Actividad actividad) {
        this.actividad = actividad;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public void setAsignatura(Asignatura asignatura) {
        this.asignatura = asignatura;
    }

    public Aula getAulaFija() {
        return aulaFija;
    }

    public void setAulaFija(Aula aulaFija) {
        this.aulaFija = aulaFija;
    }

    public Set<Profesor> getProfesores() {
        return profesores;
    }

    public void setProfesores(Set<Profesor> profesores) {
        this.profesores = profesores;
    }

    public Set<Aula> getAulasCandidatas() {
        return aulasCandidatas;
    }

    public void setAulasCandidatas(Set<Aula> aulasCandidatas) {
        this.aulasCandidatas = aulasCandidatas;
    }

    public Set<Subgrupo> getSubgrupos() {
        return subgrupos;
    }

    public void setSubgrupos(Set<Subgrupo> subgrupos) {
        this.subgrupos = subgrupos;
    }
}