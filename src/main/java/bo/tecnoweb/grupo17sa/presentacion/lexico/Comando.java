package bo.tecnoweb.grupo17sa.presentacion.lexico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private static final DateTimeFormatter FECHA_HORA_MIN = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");
    private static final DateTimeFormatter FECHA_HORA_SEG = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FECHA_HORA_BOL_MIN = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
    private static final DateTimeFormatter FECHA_HORA_BOL_SEG = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");

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

    public Integer enteroOpcional(int i) {
        String valor = parametros.get(i);
        return valor.isEmpty() ? null : Integer.valueOf(valor);
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

    public LocalDateTime fechaHora(int i) {
        return leerFechaHora(parametros.get(i));
    }

    public LocalDateTime fechaHoraOpcional(int i) {
        String valor = parametros.get(i);
        return valor.isEmpty() ? null : leerFechaHora(valor);
    }

    /** Acepta AAAA-MM-DD o DD/MM/AAAA (incluso con hora); devuelve null si no es una fecha válida. */
    public static LocalDate leerFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String t = texto.trim().split("[ T]")[0];
        try {
            return t.contains("/") ? LocalDate.parse(t, FECHA_BOLIVIANA) : LocalDate.parse(t);
        } catch (DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /** Acepta AAAA-MM-DD HH:mm, AAAA-MM-DD, DD/MM/AAAA HH:mm, DD/MM/AAAA; normaliza a LocalDateTime. */
    public static LocalDateTime leerFechaHora(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String t = texto.trim();
        if (t.contains("T")) {
            try {
                return LocalDateTime.parse(t);
            } catch (DateTimeParseException ignored) {
            }
        }
        if (t.contains(" ")) {
            String[] partes = t.split(" ");
            if (partes.length == 2) {
                try {
                    return partes[0].contains("/")
                            ? (partes[1].length() <= 5 ? LocalDateTime.parse(t, FECHA_HORA_BOL_MIN) : LocalDateTime.parse(t, FECHA_HORA_BOL_SEG))
                            : (partes[1].length() <= 5 ? LocalDateTime.parse(t, FECHA_HORA_MIN) : LocalDateTime.parse(t, FECHA_HORA_SEG));
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        LocalDate ld = leerFecha(t);
        return ld != null ? ld.atStartOfDay() : null;
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
