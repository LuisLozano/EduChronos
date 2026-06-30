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
}
