package es.yaroki.educhronos.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el manejador de los recursos estáticos (el bundle de Angular, que el pom copia a
 * classpath:/static/ en prepare-package) con ResolvedorRutasSpa como resolvedor.
 *
 * Con spring.web.resources.add-mappings=false (application.properties) Boot no registra el suyo, y este es el
 * ÚNICO manejador de /**. Medido en S155 sobre Boot 4.1.0: sin la propiedad, Boot añade /webjars/** y su
 * propio /**, y el reenvío SIGUE funcionando, porque prevalece este manejador. No se midió por qué prevalece.
 * La propiedad existe para no depender de ese detalle interno de Boot, no porque sin ella el reenvío falle hoy.
 * Se prefirió a un @Order negativo que se adelante al @Order(0) de WebMvcAutoConfiguration porque eso también
 * dependería de la implementación de Boot.
 */
@Configuration
public class RutasSpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new ResolvedorRutasSpa());
    }
}
