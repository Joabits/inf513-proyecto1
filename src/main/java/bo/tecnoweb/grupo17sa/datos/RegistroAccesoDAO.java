package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.Registro;
import bo.tecnoweb.grupo17sa.modelo.LineaAcceso;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee logs/access.log para el reporte de uso del sistema (REPUSO).
 * Formato de cada línea: fecha | remitente | asunto | ÉXITO o ERROR | título | N ms
 */
public class RegistroAccesoDAO {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern NOMBRE_COMANDO =
            Pattern.compile("^\\s*(?:(?:re|rv|fw|fwd)\\s*:\\s*)*([A-Za-z][A-Za-z0-9]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MILISEGUNDOS = Pattern.compile("(\\d+) ms$");
    private static final Pattern INICIO_ENTRADA = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\| ");

    /** Líneas de comandos procesados entre dos fechas (inclusive). */
    public List<LineaAcceso> leer(LocalDate desde, LocalDate hasta) throws IOException {
        Path archivo = Registro.carpeta().resolve("access.log");
        List<LineaAcceso> lineas = new ArrayList<>();
        if (!Files.exists(archivo)) {
            return lineas;
        }
        for (String texto : unirPartidas(Files.readAllLines(archivo, StandardCharsets.UTF_8))) {
            LineaAcceso linea = interpretar(texto);
            if (linea != null && !linea.fecha().toLocalDate().isBefore(desde) && !linea.fecha().toLocalDate().isAfter(hasta)) {
                lineas.add(linea);
            }
        }
        return lineas;
    }

    /**
     * Una línea que no empieza con la fecha es la continuación de la entrada anterior: así quedaban los errores de
     * varias líneas antes de que Registro los escribiera en una sola. Se unen para que el reporte los cuente.
     */
    static List<String> unirPartidas(List<String> lineas) {
        List<String> entradas = new ArrayList<>();
        for (String texto : lineas) {
            if (entradas.isEmpty() || INICIO_ENTRADA.matcher(texto).find()) {
                entradas.add(texto);
            } else {
                int ultima = entradas.size() - 1;
                entradas.set(ultima, entradas.get(ultima) + " " + texto.strip());
            }
        }
        return entradas;
    }

    /** Devuelve null para las líneas que no son de un comando (respuesta enviada, aviso, correo ignorado...). */
    static LineaAcceso interpretar(String texto) {
        String[] partes = texto.split(" \\| ");
        if (partes.length < 6) {
            return null;
        }
        int estado = -1;
        for (int i = 3; i < partes.length; i++) {
            if (partes[i].equals("ÉXITO") || partes[i].equals("ERROR")) {
                estado = i;
                break;
            }
        }
        if (estado < 0) {
            return null;
        }
        try {
            LocalDateTime fecha = LocalDateTime.parse(partes[0].trim(), FORMATO);
            String asunto = String.join(" | ", java.util.Arrays.copyOfRange(partes, 2, estado));
            Matcher nombre = NOMBRE_COMANDO.matcher(asunto);
            String comando = nombre.find() ? nombre.group(1).toUpperCase(Locale.ROOT) : "(asunto mal escrito)";
            Matcher ms = MILISEGUNDOS.matcher(partes[partes.length - 1].trim());
            long milisegundos = ms.find() ? Long.parseLong(ms.group(1)) : 0;
            return new LineaAcceso(fecha, partes[1].trim(), comando, partes[estado].equals("ÉXITO"), milisegundos);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
