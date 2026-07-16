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
 * Grupo administrativo (§4.1). Ej.: "1ºA", "3ºADi", "1ºBach C".
 *
 * <p>{@code grupoPadre} es una FK autorreferencial nullable: la usan los grupos
 * PDC para apuntar a su ordinario padre (invariante I5). La validación de I5 es
 * responsabilidad de la capa de configuración, no de una restricción SQL.
 */
@Entity
public class GrupoAdministrativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nivel_id", nullable = false)
    private Nivel nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoGrupo tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_padre_id")
    private GrupoAdministrativo grupoPadre;

    protected GrupoAdministrativo() {
        // requerido por JPA
    }

    public GrupoAdministrativo(String codigo, Nivel nivel, TipoGrupo tipo, GrupoAdministrativo grupoPadre) {
        this.codigo = codigo;
        this.nivel = nivel;
        this.tipo = tipo;
        this.grupoPadre = grupoPadre;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public Nivel getNivel() {
        return nivel;
    }

    public TipoGrupo getTipo() {
        return tipo;
    }

    public GrupoAdministrativo getGrupoPadre() {
        return grupoPadre;
    }

    /**
     * Reasigna código, nivel y tipo de un grupo gestionado (edición del CRUD,
     * Bloque 8.5-B). Mutación de dominio nombrada y única en lugar de setters
     * libres: la valida el servicio antes de invocarla (código no vacío, unicidad
     * de código, nivel resoluble, tipo ORDINARIO) y el flush transaccional la
     * persiste sin {@code save}. NO toca {@code id} ni {@code grupoPadre}.
     */
    public void actualizar(String codigo, Nivel nivel, TipoGrupo tipo) {
        this.codigo = codigo;
        this.nivel = nivel;
        this.tipo = tipo;
    }
}
