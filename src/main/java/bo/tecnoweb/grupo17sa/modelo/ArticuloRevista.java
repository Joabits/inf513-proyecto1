package bo.tecnoweb.grupo17sa.modelo;

/** Artículo de una edición de la revista INNOVAR (tabla revista_articulo, RF-08). */
public record ArticuloRevista(int id, int edicionId, String titulo, String autores, String resumen, String enlacePdf, int orden) {
}
