package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import bo.tecnoweb.grupo17sa.presentacion.vista.TablaHTML;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Respuestas estándar para que todos los casos de uso contesten con el mismo texto. */
public final class Respuestas {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Respuestas() {
    }

    /** "Éxito: 3 registros encontrados." con la tabla. */
    public static Respuesta listado(String titulo, TablaHTML tabla) {
        String mensaje = tabla.cantidad() == 1 ? "1 registro encontrado." : tabla.cantidad() + " registros encontrados.";
        return Respuesta.exito(titulo, mensaje).agregar(tabla);
    }

    public static Respuesta insertado(String que, Object id) {
        return Respuesta.exito("Registro insertado", "Registro insertado correctamente (" + que + " con ID " + id + ").");
    }

    public static Respuesta modificado(String que, Object id) {
        return Respuesta.exito("Registro modificado", "Registro modificado correctamente (" + que + " " + id + ").");
    }

    public static Respuesta eliminado(String que, Object id, String nombre) {
        return Respuesta.exito("Registro eliminado", "Registro eliminado correctamente (" + que + " " + id + ": «" + nombre + "»).");
    }

    public static String fecha(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA_HORA);
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }
}
