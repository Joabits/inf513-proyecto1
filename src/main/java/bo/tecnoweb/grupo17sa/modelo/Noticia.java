package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Noticia del portal web (tabla noticia, CU6). */
public record Noticia(int id, String titulo, String resumen, String contenido,
                      String imagenUrl, String autor, LocalDateTime fechaPublicacion,
                      boolean publicado, int orden, LocalDateTime creado) {

    public Noticia(int id, String titulo, String resumen, String contenido,
                   String imagenUrl, String autor, int orden) {
        this(id, titulo, resumen, contenido, imagenUrl, autor, null, true, orden, null);
    }
}
