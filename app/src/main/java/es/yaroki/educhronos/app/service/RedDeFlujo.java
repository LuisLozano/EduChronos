package es.yaroki.educhronos.app.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Red de flujo con capacidades enteras y Edmonds-Karp (caminos de aumento más cortos por BFS).
 * Implementación propia, sin dependencias. Nació dentro de {@code PrevalidacionService} para la
 * carga de aulas (S209) y sale a su clase en S213 para el reparto de guardias.
 *
 * <p>Cada arista guarda su capacidad RESIDUAL. {@link #flujoMaximo} aumenta sobre ese residual y lo
 * deja como queda, así que una segunda llamada solo AÑADE flujo: lo que ya pasaba por la red no se
 * pierde. Es lo que deja al reparto de guardias abrir aristas nuevas tras su primera fase y seguir
 * aumentando sin rehacerla.
 */
final class RedDeFlujo {

    private final List<List<int[]>> salientes = new ArrayList<>();   // {destino, capacidad, inversa}

    /** Por identificador de arista: {origen, posición en los salientes del origen, capacidad original}. */
    private final List<int[]> aristas = new ArrayList<>();

    RedDeFlujo(int nodos) {
        for (int v = 0; v < nodos; v++) {
            salientes.add(new ArrayList<>());
        }
    }

    /** Añade la arista {@code desde → hasta} y devuelve su identificador: 0, 1, 2… por orden de alta. */
    int arista(int desde, int hasta, int capacidad) {
        int posicion = salientes.get(desde).size();
        salientes.get(desde).add(new int[] {hasta, capacidad, salientes.get(hasta).size()});
        salientes.get(hasta).add(new int[] {desde, 0, posicion});
        aristas.add(new int[] {desde, posicion, capacidad});
        return aristas.size() - 1;
    }

    /** Flujo que pasa ahora por la arista {@code id}: su capacidad original menos la residual. */
    int flujo(int id) {
        int[] arista = aristas.get(id);
        return arista[2] - salientes.get(arista[0]).get(arista[1])[1];
    }

    /** Aumenta hasta el máximo y devuelve el flujo AÑADIDO en esta llamada. */
    int flujoMaximo(int fuente, int sumidero) {
        int total = 0;
        while (true) {
            int[] previo = new int[salientes.size()];
            int[] porArista = new int[salientes.size()];
            Arrays.fill(previo, -1);
            previo[fuente] = fuente;
            ArrayDeque<Integer> cola = new ArrayDeque<>(List.of(fuente));
            while (!cola.isEmpty() && previo[sumidero] < 0) {
                int u = cola.poll();
                for (int k = 0; k < salientes.get(u).size(); k++) {
                    int[] e = salientes.get(u).get(k);
                    if (e[1] > 0 && previo[e[0]] < 0) {
                        previo[e[0]] = u;
                        porArista[e[0]] = k;
                        cola.add(e[0]);
                    }
                }
            }
            if (previo[sumidero] < 0) {
                return total;
            }
            int cuello = Integer.MAX_VALUE;
            for (int v = sumidero; v != fuente; v = previo[v]) {
                cuello = Math.min(cuello, salientes.get(previo[v]).get(porArista[v])[1]);
            }
            for (int v = sumidero; v != fuente; v = previo[v]) {
                int[] e = salientes.get(previo[v]).get(porArista[v]);
                e[1] -= cuello;
                salientes.get(v).get(e[2])[1] += cuello;
            }
            total += cuello;
        }
    }

    /** Nodos alcanzables desde {@code origen} por aristas con capacidad residual. */
    boolean[] alcanzables(int origen) {
        boolean[] vistos = new boolean[salientes.size()];
        vistos[origen] = true;
        ArrayDeque<Integer> cola = new ArrayDeque<>(List.of(origen));
        while (!cola.isEmpty()) {
            for (int[] e : salientes.get(cola.poll())) {
                if (e[1] > 0 && !vistos[e[0]]) {
                    vistos[e[0]] = true;
                    cola.add(e[0]);
                }
            }
        }
        return vistos;
    }
}
