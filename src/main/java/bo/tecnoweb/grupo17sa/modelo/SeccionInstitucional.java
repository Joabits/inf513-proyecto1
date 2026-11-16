package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Sección de información institucional: historia, misión, visión... (tabla seccion_institucional, RF-02). */
public record SeccionInstitucional(String clave, String titulo, String contenido, int orden, LocalDateTime actualizado) {
}
