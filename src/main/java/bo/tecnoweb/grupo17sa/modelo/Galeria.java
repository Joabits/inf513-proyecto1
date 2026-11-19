package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Imagen de galería fotográfica con evento asociado opcional (tabla galeria, CU6). */
public record Galeria(int id, String titulo, String descripcion, String imagenUrl,
                      Integer eventoId, String eventoNombre, int orden, LocalDateTime creado) {

    public Galeria(int id, String titulo, String descripcion, String imagenUrl,
                   Integer eventoId, int orden) {
        this(id, titulo, descripcion, imagenUrl, eventoId, null, orden, null);
    }
}
