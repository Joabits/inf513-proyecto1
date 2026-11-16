package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Solicitud de servicio de una empresa o postulación de un estudiante a INNOVA (tabla innova_solicitud, RF-10). */
public record SolicitudInnova(int id, String tipo, String nombre, String correo, String telefono, String organizacion,
                              String mensaje, String estado, String observacion, LocalDateTime fecha,
                              String atendidoPor, LocalDateTime atendidoEn) {
}
