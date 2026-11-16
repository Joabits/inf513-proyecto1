package bo.tecnoweb.grupo17sa.presentacion.vista;

/** Archivo que viaja adjunto al correo de respuesta (por ejemplo, la exportación de graduados en CSV). */
public record ArchivoAdjunto(String nombre, String tipoMime, byte[] contenido) {
}
