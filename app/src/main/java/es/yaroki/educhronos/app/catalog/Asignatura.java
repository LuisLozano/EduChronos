package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Asignatura (§4.1). El {@code codigo} es la clave natural ("Mat", "LCL").
 *
 * <p><b>Con nivel o sin él</b> (S250, C-asignatura-con-nivel). Una asignatura con nivel lleva
 * como código el corto, un guion y el código del nivel ({@code Biol-3ESO}), y el corto se guarda
 * aparte; la que no tiene nivel conserva su código, que es también su corto. El código se
 * compone siempre con {@link #componerCodigo}, y es lo que siguen usando como clave las
 * actividades, las plazas y el solver; el corto es lo que enseñan la rejilla y la exportación.
 */
@Entity
public class Asignatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombreCompleto;

    @Column(nullable = false)
    private String codigoCorto;

    // Opcional: null si la asignatura no es de ningún nivel (reuniones, funciones).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_id")                 // nullable por defecto
    private Nivel nivel;

    protected Asignatura() {
        // requerido por JPA
    }

    /** Sin nivel: el código es también el corto. */
    public Asignatura(String codigo, String nombreCompleto) {
        this(codigo, nombreCompleto, null);
    }

    /** Con nivel, o sin él si {@code nivel} es null: el código se compone. */
    public Asignatura(String codigoCorto, String nombreCompleto, Nivel nivel) {
        this.codigoCorto = codigoCorto;
        this.nombreCompleto = nombreCompleto;
        this.nivel = nivel;
        recomponerCodigo();
    }

    /**
     * El código de una asignatura con ese corto y ese nivel: {@code corto-nivel}, o el corto solo
     * si no hay nivel. Es la ÚNICA regla de composición: la usan la entidad, el servicio para
     * comprobar la unicidad antes de escribir y {@code NivelService} al renombrar un nivel.
     *
     * @param codigoCorto el código corto
     * @param codigoNivel el código del nivel, o {@code null} sin nivel
     */
    public static String componerCodigo(String codigoCorto, String codigoNivel) {
        return codigoNivel == null ? codigoCorto : codigoCorto + "-" + codigoNivel;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getCodigoCorto() {
        return codigoCorto;
    }

    public Nivel getNivel() {
        return nivel;
    }

    /**
     * Reasigna corto, nombre y nivel de una asignatura gestionada (edición del CRUD,
     * Bloque 8.5-A; el nivel desde S250) y recompone el código. Mutación de dominio nombrada y
     * única en lugar de setters libres: la valida el servicio antes de invocarla (corto y nombre
     * no vacíos, unicidad del código compuesto) y el flush transaccional la persiste sin
     * {@code save}.
     */
    public void actualizar(String codigoCorto, String nombreCompleto, Nivel nivel) {
        this.codigoCorto = codigoCorto;
        this.nombreCompleto = nombreCompleto;
        this.nivel = nivel;
        recomponerCodigo();
    }

    /**
     * Vuelve a componer el código con el corto y el código ACTUAL del nivel. Lo llama
     * {@code NivelService} después de renombrar el nivel, en la misma transacción.
     */
    public void recomponerCodigo() {
        this.codigo = componerCodigo(codigoCorto, nivel == null ? null : nivel.getCodigo());
    }
}
