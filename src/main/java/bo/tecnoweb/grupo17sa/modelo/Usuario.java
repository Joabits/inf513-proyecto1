package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Usuario del sistema, identificado por su correo (tabla usuario, RF-18). rol es uno de los tres roles fijos. */
public record Usuario(int id, String correo, String nombre, Rol rol, boolean activo, LocalDateTime creado) {
}
