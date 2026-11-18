package bo.tecnoweb.grupo17sa.modelo;

import java.util.List;

/** Coincidencias de la búsqueda global en una sección del portal (RF-17); total cuenta todas, coincidencias trae las primeras. */
public record ResultadoBusqueda(String modulo, String seccion, int total, List<Coincidencia> coincidencias) {

    public record Coincidencia(String titulo, String detalle) {
    }
}
