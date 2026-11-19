package bo.tecnoweb.grupo17sa;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logs locales que pide el enunciado, en la carpeta logs/:
 *  - access.log: cada correo procesado y cada comando ejecutado, con su resultado.
 *  - error.log: las excepciones capturadas, con su traza.
 */
public final class Registro {

    private static final Path CARPETA = Path.of(System.getProperty("registro.carpeta", "logs"));
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Registro() {
    }

    /** Carpeta de los logs (logs/ salvo que se indique otra con -Dregistro.carpeta). */
    public static Path carpeta() {
        return CARPETA;
    }

    public static synchronized void acceso(String mensaje) {
        escribir("access.log", unaLinea(mensaje));
    }

    /**
     * access.log tiene una entrada por línea. El detalle de un error de PostgreSQL puede traer saltos de línea
     * ("... no existe la relación «noticia»\n  Position: 108"): se cambian por un espacio para no partir la entrada.
     */
    public static String unaLinea(String mensaje) {
        return mensaje.replaceAll("\\s*\\R\\s*", " ");
    }

    public static synchronized void error(String mensaje, Throwable excepcion) {
        String texto = mensaje;
        if (excepcion != null) {
            StringWriter traza = new StringWriter();
            excepcion.printStackTrace(new PrintWriter(traza));
            texto += System.lineSeparator() + traza;
        }
        escribir("error.log", texto);
    }

    private static void escribir(String archivo, String mensaje) {
        String linea = LocalDateTime.now().format(FORMATO) + " | " + mensaje + System.lineSeparator();
        try {
            Files.createDirectories(CARPETA);
            Files.writeString(CARPETA.resolve(archivo), linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("No se pudo escribir en " + CARPETA.resolve(archivo) + ": " + e.getMessage());
        }
    }
}
