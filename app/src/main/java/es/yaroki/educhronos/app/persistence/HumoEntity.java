package es.yaroki.educhronos.app.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Entidad de humo trivial (Bloque 1, Fase 6).
 *
 * <p>NO pertenece al modelo de datos real. Existe únicamente para forzar que
 * Hibernate genere una tabla y así verificar de extremo a extremo que el DDL
 * sobre SQLite funciona. Se eliminará cuando entren las entidades reales.
 */
@Entity
public class HumoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nota;

    protected HumoEntity() {
        // requerido por JPA
    }

    public Long getId() {
        return id;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }
}
