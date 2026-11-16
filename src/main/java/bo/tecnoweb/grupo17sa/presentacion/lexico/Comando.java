package bo.tecnoweb.grupo17sa.presentacion.lexico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Comando ya analizado: el nombre en mayúsculas y los parámetros sin comillas.
 * Los métodos entero(), fecha(), etc. convierten un parámetro que el Despachador ya validó.
 */
public final class Comando {

    private static final DateTimeFormatter FECHA_BOLIVIANA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final String nombre;
    private final List<String> parametros;

    public Comando(String nombre, List<String> parametros) {
        this.nombre = nombre;
        this.parametros = List.copyOf(parametros);
    }

    public String nombre() {
        return nombre;
    }

    public List<String> parametros() {
        return parametros;
    }

    public int cantidad() {
        return parametros.size();
    }

    /** Parámetro i (desde 0) tal como llegó. */
    public String parametro(int i) {
        return parametros.get(i);
    }

    /** Parámetro i como texto; null si llegó vacío (""), para guardarlo como NULL. */
    public String texto(int i) {
        String valor = parametros.get(i);
        return valor.isEmpty() ? null : valor;
    }

    public int entero(int i) {
        return Integer.parseInt(parametros.get(i));
    }

    public BigDecimal decimal(int i) {
        return new BigDecimal(parametros.get(i).replace(',', '.'));
    }

    public boolean booleano(int i) {
        String valor = parametros.get(i).toUpperCase(Locale.ROOT);
        return valor.equals("SI") || valor.equals("SÍ") || valor.equals("TRUE");
    }

    public LocalDate fecha(int i) {
        return leerFecha(parametros.get(i));
    }

    /** Acepta AAAA-MM-DD o DD/MM/AAAA; devuelve null si no es una fecha válida. */
    public static LocalDate leerFecha(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return texto.contains("/") ? LocalDate.parse(texto.trim(), FECHA_BOLIVIANA) : LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        if (parametros.isEmpty()) {
            return nombre;
        }
        StringBuilder sb = new StringBuilder(nombre).append('[');
        for (int i = 0; i < parametros.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(parametros.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        return sb.append(']').toString();
    }
}
