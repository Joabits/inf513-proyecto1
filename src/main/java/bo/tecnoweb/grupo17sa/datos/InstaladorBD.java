package bo.tecnoweb.grupo17sa.datos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Crea las tablas y carga los datos iniciales (opción --instalar-bd). Se puede ejecutar varias veces. */
public final class InstaladorBD {

    /** Scripts de src/main/resources/sql, en orden. Cada caso de uso agrega aquí el suyo. */
    private static final String[] SCRIPTS = {
            "sql/cu2_portal.sql"
    };

    private InstaladorBD() {
    }

    public static void instalar() throws IOException, SQLException {
        System.out.println("Base de datos: " + ConexionDB.probar());
        try (Connection con = ConexionDB.obtener(); Statement st = con.createStatement()) {
            con.setAutoCommit(false);
            try {
                for (String script : SCRIPTS) {
                    List<String> sentencias = dividir(leerScript(script));
                    for (String sentencia : sentencias) {
                        st.execute(sentencia);
                    }
                    System.out.println("  " + script + ": " + sentencias.size() + " sentencias ejecutadas");
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
        System.out.println("Instalación terminada.");
    }

    public static String leerScript(String ruta) throws IOException {
        try (InputStream entrada = InstaladorBD.class.getClassLoader().getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new IOException("No se encontró el script " + ruta);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Separa el script en sentencias: cada una termina con ';' al final de una línea. Ignora los comentarios "--". */
    public static List<String> dividir(String script) {
        List<String> sentencias = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String linea : script.split("\\R")) {
            String limpia = linea.strip();
            if (limpia.isEmpty() || limpia.startsWith("--")) {
                continue;
            }
            actual.append(linea).append('\n');
            if (limpia.endsWith(";")) {
                String sentencia = actual.toString().strip();
                sentencias.add(sentencia.substring(0, sentencia.length() - 1));
                actual.setLength(0);
            }
        }
        if (!actual.toString().isBlank()) {
            sentencias.add(actual.toString().strip());
        }
        return sentencias;
    }
}
