package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Acción de un usuario administrador (tabla bitacora, RF-19). */
public record RegistroBitacora(long id, LocalDateTime fecha, String usuario, String accion, String modulo,
                               String tabla, String elemento, String detalle) {
}
