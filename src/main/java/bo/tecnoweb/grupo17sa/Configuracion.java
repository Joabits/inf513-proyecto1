package bo.tecnoweb.grupo17sa;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Lee config.properties: datos de conexión y contraseñas (ese archivo no se sube a GitHub). */
public final class Configuracion {

    private static final Properties PROPIEDADES = new Properties();

    private Configuracion() {
    }

    public static void cargar(String ruta) throws IOException {
        try (Reader lector = Files.newBufferedReader(Path.of(ruta), StandardCharsets.UTF_8)) {
            PROPIEDADES.clear();
            PROPIEDADES.load(lector);
        }
    }

    public static String texto(String clave) {
        return PROPIEDADES.getProperty(clave, "").trim();
    }

    public static int entero(String clave, int porDefecto) {
        try {
            return Integer.parseInt(texto(clave));
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    /** Valores separados por comas, en minúsculas y sin espacios. */
    public static List<String> lista(String clave) {
        List<String> valores = new ArrayList<>();
        for (String valor : texto(clave).split(",")) {
            if (!valor.isBlank()) {
                valores.add(valor.trim().toLowerCase(Locale.ROOT));
            }
        }
        return valores;
    }
}
