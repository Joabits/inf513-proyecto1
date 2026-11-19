package bo.tecnoweb.grupo17sa.datos;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptsSqlTest {

    private static final List<String> SCRIPTS = List.of("sql/cu1_usuarios.sql", "sql/cu2_portal.sql",
            "sql/cu3_academica.sql", "sql/cu4_investigacion.sql", "sql/cu5_vinculacion.sql", "sql/cu6_noticias.sql",
            "sql/cu7_contacto.sql");

    @Test
    void elScriptDelCu2SeDivideEnSentenciasCompletas() throws IOException {
        List<String> sentencias = InstaladorBD.dividir(InstaladorBD.leerScript("sql/cu2_portal.sql"));
        assertEquals(21, sentencias.size());
        for (String sentencia : sentencias) {
            assertTrue(sentencia.startsWith("CREATE") || sentencia.startsWith("ALTER") || sentencia.startsWith("INSERT"), sentencia);
            assertTrue(!sentencia.endsWith(";"), sentencia);
        }
    }

    @Test
    void elScriptDelCu3SeDivideEnSentenciasCompletas() throws IOException {
        List<String> sentencias = InstaladorBD.dividir(InstaladorBD.leerScript("sql/cu3_academica.sql"));
        assertEquals(9, sentencias.size());
        for (String sentencia : sentencias) {
            assertTrue(sentencia.startsWith("CREATE") || sentencia.startsWith("ALTER") || sentencia.startsWith("INSERT"), sentencia);
            assertTrue(!sentencia.endsWith(";"), sentencia);
        }
    }

    @Test
    void elScriptDelCu5SeDivideEnSentenciasCompletas() throws IOException {
        List<String> sentencias = InstaladorBD.dividir(InstaladorBD.leerScript("sql/cu5_vinculacion.sql"));
        assertEquals(20, sentencias.size());
        for (String sentencia : sentencias) {
            assertTrue(sentencia.startsWith("CREATE") || sentencia.startsWith("ALTER") || sentencia.startsWith("INSERT"), sentencia);
        }
    }

    @Test
    void elScriptDelCu6SeDivideEnSentenciasCompletas() throws IOException {
        List<String> sentencias = InstaladorBD.dividir(InstaladorBD.leerScript("sql/cu6_noticias.sql"));
        assertEquals(12, sentencias.size());
        long createTables = sentencias.stream().filter(s -> s.startsWith("CREATE TABLE")).count();
        assertEquals(4, createTables);
        for (String sentencia : sentencias) {
            assertTrue(sentencia.startsWith("CREATE") || sentencia.startsWith("ALTER") || sentencia.startsWith("INSERT"), sentencia);
            assertTrue(!sentencia.endsWith(";"), sentencia);
        }
    }

    /**
     * Ninguna tabla queda suelta: fuera de las del CU1 (rol, rol_modulo, usuario), cada tabla tiene usuario_id
     * en su CREATE TABLE y su ALTER TABLE para bases ya instaladas, y ResponsableDAO sabe llenarla
     * (salvo bitacora, que guarda su usuario_id al insertarse).
     */
    @Test
    void cadaTablaTieneSuResponsable() throws IOException {
        Pattern crear = Pattern.compile("CREATE TABLE IF NOT EXISTS (\\w+) \\(([\\s\\S]*)\\)");
        Pattern alterar = Pattern.compile("ALTER TABLE (\\w+) ADD COLUMN IF NOT EXISTS usuario_id INTEGER REFERENCES usuario \\(id\\) ON DELETE SET NULL");
        Set<String> tablas = new TreeSet<>();
        Set<String> conColumna = new TreeSet<>();
        Set<String> conAlter = new TreeSet<>();
        for (String script : SCRIPTS) {
            for (String sentencia : InstaladorBD.dividir(InstaladorBD.leerScript(script))) {
                Matcher c = crear.matcher(sentencia);
                if (c.matches()) {
                    tablas.add(c.group(1));
                    if (c.group(2).matches("(?s).*\\n\\s+usuario_id\\s+INTEGER\\s+REFERENCES usuario \\(id\\) ON DELETE SET NULL.*")) {
                        conColumna.add(c.group(1));
                    }
                }
                Matcher a = alterar.matcher(sentencia);
                if (a.matches()) {
                    conAlter.add(a.group(1));
                }
            }
        }
        Set<String> esperadas = new TreeSet<>(tablas);
        esperadas.removeAll(Set.of("rol", "rol_modulo", "usuario"));
        assertEquals(26, esperadas.size());
        assertEquals(esperadas, conColumna);
        assertEquals(esperadas, conAlter);

        Set<String> llenadas = new TreeSet<>(ResponsableDAO.tablas());
        llenadas.add("bitacora");
        assertEquals(esperadas, llenadas);
    }

    @Test
    void ignoraComentariosYUneLineas() {
        List<String> sentencias = InstaladorBD.dividir("-- comentario\nCREATE TABLE a (\n  id INT\n);\n\nINSERT INTO a VALUES (1);");
        assertEquals(List.of("CREATE TABLE a (\n  id INT\n)", "INSERT INTO a VALUES (1)"), sentencias);
    }
}
