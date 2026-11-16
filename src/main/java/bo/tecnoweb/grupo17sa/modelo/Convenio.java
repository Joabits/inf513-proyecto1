package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDate;

/** Convenio, práctica, proyecto con empresa, consultoría o actividad de interacción social (tabla convenio, RF-09). */
public record Convenio(int id, String tipo, String institucion, String objeto, LocalDate fechaInicio,
                       LocalDate fechaFin, String estado, String responsable, String logoUrl) {
}
