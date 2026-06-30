package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Parámetro global clave-valor (§4.1): pesos del solver, jornada, etc.
 *
 * <p>Excepción a la convención de id sintético: la {@code clave} es la clave
 * natural y primaria, porque esta entidad ES un almacén clave-valor.
 */
@Entity
public class Configuracion {

    @Id
    private String clave;

    @Column(nullable = false)
    private String valor;

    protected Configuracion() {
        // requerido por JPA
    }

    public Configuracion(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public String getClave() {
        return clave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
