package es.yaroki.educhronos.app.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Aula (§4.1). Lleva todos los campos del modelo, incluidos
 * edificio/planta/sector/capacidad (nullable), que la fórmula de distancia
 * consumirá en una fase posterior. La TABLA de overrides de distancia NO entra
 * en este bloque.
 */
@Entity
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAula tipo;

    @Column(nullable = true)
    private Integer capacidad;

    @Column(nullable = true)
    private String edificio;

    /** 0=baja, 1=primera, 2=segunda. */
    @Column(nullable = true)
    private Integer planta;

    @Column(nullable = true)
    private String sector;

    protected Aula() {
        // requerido por JPA
    }

    public Aula(String codigo, TipoAula tipo, Integer capacidad,
                String edificio, Integer planta, String sector) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.edificio = edificio;
        this.planta = planta;
        this.sector = sector;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoAula getTipo() {
        return tipo;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public String getEdificio() {
        return edificio;
    }

    public Integer getPlanta() {
        return planta;
    }

    public String getSector() {
        return sector;
    }

    /**
     * Reasigna todos los campos editables de un aula gestionada (edición del CRUD,
     * Bloque 8.5-A'). Mutación de dominio nombrada y única en lugar de setters
     * libres: la valida el servicio antes de invocarla (código no vacío, tipo no
     * vacío y parseable; capacidad/edificio/planta/sector son nullable de verdad y
     * no se validan) y el flush transaccional la persiste sin {@code save}.
     */
    public void actualizar(String codigo, TipoAula tipo, Integer capacidad,
                           String edificio, Integer planta, String sector) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.edificio = edificio;
        this.planta = planta;
        this.sector = sector;
    }
}
