package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Documento del repositorio (tabla documento, RF-06). El archivo está en el servidor web; aquí va su enlace. */
public record Documento(int id, String titulo, String descripcion, String categoria, String enlace, boolean publicado,
                        LocalDateTime creado) {
}
