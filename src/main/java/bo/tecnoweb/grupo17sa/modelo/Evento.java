package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Evento institucional o académico (tabla evento, CU6). */
public record Evento(int id, String titulo, String descripcion, LocalDateTime fechaInicio,
                     LocalDateTime fechaFin, String lugar, String imagenUrl,
                     LocalDateTime creado) {

    public Evento(int id, String titulo, String descripcion, LocalDateTime fechaInicio,
                  LocalDateTime fechaFin, String lugar, String imagenUrl) {
        this(id, titulo, descripcion, fechaInicio, fechaFin, lugar, imagenUrl, null);
    }
}
