package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Información académica general y reglamentos (tabla info_academica, CU3). */
public record InfoAcademica(int id, String titulo, String contenido, int orden, LocalDateTime actualizado) {

    public InfoAcademica(int id, String titulo, String contenido, int orden) {
        this(id, titulo, contenido, orden, null);
    }
}
