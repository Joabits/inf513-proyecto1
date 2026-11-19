package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Mensaje enviado a la Carrera con el formulario de contacto (tabla mensaje_contacto, RF-15). */
public record MensajeContacto(int id, String correo, String nombre, String telefono, String asunto, String mensaje,
                              String estado, String respuesta, String respondidoPor, LocalDateTime respondidoEn,
                              LocalDateTime fecha) {
}
