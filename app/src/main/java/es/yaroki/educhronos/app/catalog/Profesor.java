package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Profesor (§4.1). El {@code codigo} es la clave natural ("MAT8", "LEN2").
 */
@Entity
public class Profesor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombreCompleto;

    /** Horas de clase semanales declaradas (S203); null si no se han declarado. */
    @Column(name = "total_declarado")
    private Integer totalDeclarado;

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo", nullable = false)
    private Cargo cargo = Cargo.PROFESOR;

    protected Profesor() {
        // requerido por JPA
    }

    public Profesor(String codigo, String nombreCompleto) {
        this.codigo = codigo;
        this.nombreCompleto = nombreCompleto;
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

    public Integer getTotalDeclarado() {
        return totalDeclarado;
    }

    public void setTotalDeclarado(Integer totalDeclarado) {
        this.totalDeclarado = totalDeclarado;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    /**
     * Reasigna código y nombre de un profesor gestionado (edición del CRUD,
     * Bloque 8.5-A'). Mutación de dominio nombrada y única en lugar de setters
     * libres: la valida el servicio antes de invocarla (código y nombre no vacíos,
     * unicidad de código) y el flush transaccional la persiste sin {@code save}.
     */
    public void actualizar(String codigo, String nombreCompleto) {
        this.codigo = codigo;
        this.nombreCompleto = nombreCompleto;
    }
}
