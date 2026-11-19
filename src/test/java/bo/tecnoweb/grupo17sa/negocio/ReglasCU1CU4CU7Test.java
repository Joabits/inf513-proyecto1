package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.InstaladorBD;
import bo.tecnoweb.grupo17sa.modelo.Rol;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reglas de CU1, CU4 y CU7 que no necesitan base de datos, y sus scripts SQL. */
class ReglasCU1CU4CU7Test {

    @Test
    void losScriptsSeDividenEnSentenciasCompletas() throws IOException {
        assertSentencias("sql/cu1_usuarios.sql", 5);
        assertSentencias("sql/cu4_investigacion.sql", 12);
        assertSentencias("sql/cu7_contacto.sql", 4);
    }

    private static void assertSentencias(String script, int cantidad) throws IOException {
        List<String> sentencias = InstaladorBD.dividir(InstaladorBD.leerScript(script));
        assertEquals(cantidad, sentencias.size(), script);
        for (String sentencia : sentencias) {
            assertTrue(sentencia.matches("(?s)(CREATE|ALTER|INSERT|DROP TABLE IF EXISTS|DO \\$\\$)\\s.*"), sentencia);
        }
    }

    /** La migración de los roles dinámicos a los fijos es una sola sentencia, con sus ';' internos. */
    @Test
    void elScriptDelCu1MigraLosRolesDinamicosEnUnBloque() throws IOException {
        List<String> sentencias = InstaladorBD.dividir(InstaladorBD.leerScript("sql/cu1_usuarios.sql"));
        String bloque = sentencias.get(1);
        assertTrue(bloque.startsWith("DO $$") && bloque.endsWith("$$"), bloque);
        assertTrue(bloque.contains("DROP COLUMN rol_id;"), bloque);
        assertEquals(List.of("DROP TABLE IF EXISTS rol_modulo", "DROP TABLE IF EXISTS rol"), sentencias.subList(2, 4));
    }

    @Test
    void elRolSeLeeSinImportarComoSeEscriba() throws ErrorNegocio {
        assertEquals(Rol.EDITOR_COMUNICACION, UsuariosNegocio.leerRol("Editor de Comunicación"));
        assertEquals(Rol.EDITOR_COMUNICACION, UsuariosNegocio.leerRol("EDITOR_COMUNICACION"));
        assertEquals(Rol.EDITOR_INSTITUCIONAL, UsuariosNegocio.leerRol("  editor institucional "));
        assertEquals(Rol.ADMINISTRADOR, UsuariosNegocio.leerRol("administrador"));
        assertThrows(ErrorNegocio.class, () -> UsuariosNegocio.leerRol("CONSULTOR"), "el rol CONSULTOR ya no existe");
        assertThrows(ErrorNegocio.class, () -> UsuariosNegocio.leerRol("VISITANTE"), "el visitante no es un usuario");
        assertThrows(ErrorNegocio.class, () -> UsuariosNegocio.leerRol("editor"));
    }

    @Test
    void lasPalabrasDeBusquedaVanSinTildesNiRepetidas() {
        assertEquals(List.of("reglamento", "de", "titulacion"), BusquedaNegocio.palabras("Reglamento  de TITULACIÓN de"));
        assertEquals(List.of("año", "academico"), BusquedaNegocio.palabras("año académico"), "la ñ se conserva");
        assertEquals(List.of("revista"), BusquedaNegocio.palabras("a revista y"), "las letras sueltas no cuentan");
        assertEquals(5, BusquedaNegocio.palabras("uno dos tres cuatro cinco seis siete").size());
    }

    @Test
    void laLineaYElVolumenSonOpcionales() throws ErrorNegocio {
        assertNull(InvestigacionNegocio.leerLinea(""));
        assertEquals(3, InvestigacionNegocio.leerLinea(" 3 "));
        assertThrows(ErrorNegocio.class, () -> InvestigacionNegocio.leerLinea("tres"));
        assertNull(RevistaNegocio.leerVolumen(null));
        assertEquals(2, RevistaNegocio.leerVolumen("2"));
        assertThrows(ErrorNegocio.class, () -> RevistaNegocio.leerVolumen("0"));
    }
}
