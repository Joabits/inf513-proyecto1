package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Graduado de la Carrera registrado en el programa Alumni (tabla graduado, RF-11). */
public record Graduado(int id, String correo, String nombre, String apellidos, String telefono, int anioTitulacion,
                       String empleoActual, String cargo, String sector, String ciudad, LocalDateTime actualizado) {
}
