package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Nivel educativo (§4.1). Ej.: "1ESO", "2ESO", "1BACH", "1FPB".
 */
@Entity
public class Nivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    /** Entero para ordenación en UI. */
    @Column(nullable = false)
    private int orden;

    protected Nivel() {
        // requerido por JPA
    }

    public Nivel(String codigo, int orden) {
        this.codigo = codigo;
        this.orden = orden;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public int getOrden() {
        return orden;
    }
}
