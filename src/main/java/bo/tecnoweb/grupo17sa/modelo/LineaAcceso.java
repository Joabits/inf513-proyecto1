package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Una línea de logs/access.log: un correo procesado y su resultado. */
public record LineaAcceso(LocalDateTime fecha, String correo, String comando, boolean exito, long milisegundos) {
}
