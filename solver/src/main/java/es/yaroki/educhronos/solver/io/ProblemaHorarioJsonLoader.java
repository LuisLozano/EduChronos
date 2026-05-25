package es.yaroki.educhronos.solver.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Deserializa el JSON de entrada a DTOs (Jackson) y delega en el mapper la construccion del dominio. */
public final class ProblemaHorarioJsonLoader {

    private final ObjectMapper objectMapper;

    public ProblemaHorarioJsonLoader() {
        // FAIL_ON_UNKNOWN_PROPERTIES ya es el valor por defecto; explicito por intencion:
        // una clave desconocida en el JSON debe fallar, no silenciarse (coherente con additionalProperties:false).
        this.objectMapper = new ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public ProblemaHorario cargar(InputStream entrada) {
        Objects.requireNonNull(entrada, "entrada no puede ser null");
        ProblemaHorarioDto dto;
        try {
            dto = objectMapper.readValue(entrada, ProblemaHorarioDto.class);
        } catch (IOException e) {
            throw new ProblemaInvalidoException("JSON ilegible o malformado: " + e.getMessage(), e);
        }
        if (dto == null) {
            throw new ProblemaInvalidoException("el JSON esta vacio");
        }
        return ProblemaHorarioMapper.aDominio(dto);
    }
}