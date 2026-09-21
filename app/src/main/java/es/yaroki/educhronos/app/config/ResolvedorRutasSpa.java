package es.yaroki.educhronos.app.config;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Sirve index.html para las rutas de la SPA que no son un fichero, para que F5 y una URL directa a una
 * vista funcionen (condición 9 de O-instalación, salda D-spa-sin-fallback-de-rutas). La SPA usa
 * PathLocationStrategy (base href "/", sin withHashLocation), así que sus URLs son rutas reales.
 *
 * Devuelve el recurso pedido si existe. Si no existe, devuelve index.html SALVO en dos casos, que deben
 * seguir dando 404:
 *  - rutas de la API ("api/..."): un endpoint inexistente es un error de la API, no una vista. El
 *    Accept no sirve para distinguirlas (medido en S155: /api/jornada con Accept text/html da 406);
 *  - rutas con extensión en su último segmento: un .js o .css que falta no puede recibir HTML.
 *
 * Medido en S155 (M4 sobre el jar): la ruta llega SIN barra inicial, relativa al patrón /**.
 * Lo registra RutasSpaConfig; allí se explica por qué es el único manejador de /**.
 */
public class ResolvedorRutasSpa extends PathResourceResolver {

    static final String PREFIJO_API = "api/";
    static final String INDICE = "index.html";

    @Override
    protected Resource getResource(String ruta, Resource ubicacion) throws IOException {
        Resource pedido = super.getResource(ruta, ubicacion);
        if (pedido != null) {
            return pedido;
        }
        if (esRutaDeApi(ruta) || tieneExtension(ruta)) {
            return null;
        }
        return super.getResource(INDICE, ubicacion);
    }

    static boolean esRutaDeApi(String ruta) {
        return ruta.startsWith(PREFIJO_API);
    }

    static boolean tieneExtension(String ruta) {
        int ultimaBarra = ruta.lastIndexOf('/');
        return ruta.indexOf('.', ultimaBarra + 1) >= 0;
    }
}
