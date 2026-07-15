package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Asignatura (§4.1). El {@code codigo} es la clave natural ("Mat", "LCL").
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

    protected Asignatura() {
        // requerido por JPA
    }

    public Asignatura(String codigo, String nombreCompleto) {
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

    /**
     * Reasigna código y nombre de una asignatura gestionada (edición del CRUD,
     * Bloque 8.5-A). Mutación de dominio nombrada y única en lugar de setters
     * libres: la valida el servicio antes de invocarla (código y nombre no vacíos,
     * unicidad de código) y el flush transaccional la persiste sin {@code save}.
     */
    public void actualizar(String codigo, String nombreCompleto) {
        this.codigo = codigo;
        this.nombreCompleto = nombreCompleto;
    }
}
