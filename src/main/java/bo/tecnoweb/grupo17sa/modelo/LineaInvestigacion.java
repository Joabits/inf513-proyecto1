package bo.tecnoweb.grupo17sa.modelo;

/** Línea de investigación (tabla investigacion_linea, RF-07); proyectos cuenta los publicados de la línea. */
public record LineaInvestigacion(int id, String nombre, String descripcion, int proyectos) {
}
