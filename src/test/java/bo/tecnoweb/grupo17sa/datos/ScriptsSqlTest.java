package bo.tecnoweb.grupo17sa.datos;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptsSqlTest {

    @Test
    void elScriptDelCu2SeDivideEnSentenciasCompletas() throws IOException {
        List<String> sentencias = InstaladorBD.dividir(InstaladorBD.leerScript("sql/cu2_portal.sql"));
        assertEquals(14, sentencias.size());
        for (String sentencia : sentencias) {
            assertTrue(sentencia.startsWith("CREATE") || sentencia.startsWith("INSERT"), sentencia);
            assertTrue(!sentencia.endsWith(";"), sentencia);
        }
    }

    @Test
    void ignoraComentariosYUneLineas() {
        List<String> sentencias = InstaladorBD.dividir("-- comentario\nCREATE TABLE a (\n  id INT\n);\n\nINSERT INTO a VALUES (1);");
        assertEquals(List.of("CREATE TABLE a (\n  id INT\n)", "INSERT INTO a VALUES (1)"), sentencias);
    }
}
