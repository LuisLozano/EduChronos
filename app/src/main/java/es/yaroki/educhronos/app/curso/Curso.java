package es.yaroki.educhronos.app.curso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Identidad del fichero de base de datos: cómo se llama el curso que contiene y si está
 * archivado (O-curso, S159).
 *
 * <p><b>Fila única.</b> El {@code id} es siempre 1 y el {@code schema.sql} lo fija con un
 * {@code check (id = 1)} añadido a mano, como las FK: el dialecto no emite checks de ese
 * tipo. Es la PRIMERA tabla de fila única del proyecto; la alternativa era meter dos claves
 * en {@code configuracion}, y se descartó porque esto NO es configuración —no se copia tal
 * cual al duplicar, se reescribe— y porque un nombre de curso con tipo es lo que permite
 * validarlo en el mapeo.
 *
 * <p>Excepción a la convención de id sintético igual que {@code Configuracion}: aquí el id
 * no identifica una entre muchas, es una constante.
 *
 * <p><b>Sin fila = curso sin nombre y no archivado</b> (condición 6): una base que viene de
 * antes de S159 se abre sin intervención y el {@code GET} responde {@code nombre: null}.
 */
@Entity
@Table(name = "curso")
public class Curso {

    /** Nombre del curso, {@code 2026/2027}: nueve caracteres exactos, de ahí el length. */
    static final int LONGITUD_NOMBRE = 9;

    /** La única fila que puede existir. */
    public static final int ID = 1;

    @Id
    private Integer id;

    @Column(nullable = false, length = LONGITUD_NOMBRE)
    private String nombre;

    @Column(nullable = false)
    private boolean archivado;

    protected Curso() {
        // requerido por JPA
    }

    public Curso(String nombre, boolean archivado) {
        this.id = ID;
        this.nombre = nombre;
        this.archivado = archivado;
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isArchivado() {
        return archivado;
    }
}
