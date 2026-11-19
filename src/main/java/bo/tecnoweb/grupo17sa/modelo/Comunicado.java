package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Comunicado o aviso oficial (tabla comunicado, CU6). */
public record Comunicado(int id, String titulo, String contenido, String prioridad,
                         LocalDateTime fechaPublicacion, LocalDateTime fechaExpiracion,
                         LocalDateTime creado) {

    public Comunicado(int id, String titulo, String contenido, String prioridad,
                      LocalDateTime fechaExpiracion) {
        this(id, titulo, contenido, prioridad, null, fechaExpiracion, null);
    }
}
