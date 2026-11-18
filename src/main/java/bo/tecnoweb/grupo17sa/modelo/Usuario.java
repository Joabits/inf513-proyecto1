package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Usuario del sistema, identificado por su correo (tabla usuario, RF-18). rol es el nombre del rol. */
public record Usuario(int id, String correo, String nombre, int rolId, String rol, boolean activo, LocalDateTime creado) {
}
