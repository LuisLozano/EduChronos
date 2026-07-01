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
import java.util.HashSet;
import java.util.Set;

/**
 * Subgrupo de alumnos (§4.2). Unidad atómica del scheduling: el conjunto
 * persistente de alumnos que se desplaza junto durante el curso.
 *
 * <p>Es una entidad de primera clase con identidad propia, independiente de la
 * partición en la que se declare (Hallazgo D, invariante I6). Su población —los
 * grupos administrativos que contribuyen al subgrupo— se define aquí una sola vez
 * vía la relación N:M {@code grupos} (materializa {@code SubgrupoGrupo} de §4.2),
 * no por partición.
 *
 * <p>Un subgrupo mono-grupo (el caso ordinario) tiene un único elemento en
 * {@code grupos}; un subgrupo multi-grupo (p.ej. la "Lectura B" de Bachillerato,
 * cuyos alumnos provienen de varios grupos del nivel) tiene varios.
 *
 * <p>{@code Particion} y {@code SubgrupoParticion} (§4.2) NO se materializan en
 * esta fase: el dominio del solver no las consume y su UX se diseña con la UI de
 * Fase 8 (deudas D1 y D7). Ver decisión D-a del Bloque 4 (S48).
 */
@Entity
public class Subgrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "subgrupo_grupo",
            joinColumns = @JoinColumn(name = "subgrupo_id"),
            inverseJoinColumns = @JoinColumn(name = "grupo_id"))
    private Set<GrupoAdministrativo> grupos = new HashSet<>();

    protected Subgrupo() {
        // requerido por JPA
    }

    public Subgrupo(String codigo, Set<GrupoAdministrativo> grupos) {
        this.codigo = codigo;
        this.grupos = new HashSet<>(grupos);
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Set<GrupoAdministrativo> getGrupos() {
        return grupos;
    }
}