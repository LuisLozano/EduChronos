package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
}
