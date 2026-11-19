package bo.tecnoweb.grupo17sa.modelo;

import java.time.LocalDateTime;

/** Proyecto de investigación, de aula (PIA) o de semillero (tabla investigacion_proyecto, RF-07). linea es el nombre de la línea. */
public record ProyectoInvestigacion(int id, String titulo, String tipo, Integer lineaId, String linea, String responsable,
                                    String integrantes, int gestion, String estado, String resumen, boolean publicado,
                                    LocalDateTime creado) {
}
