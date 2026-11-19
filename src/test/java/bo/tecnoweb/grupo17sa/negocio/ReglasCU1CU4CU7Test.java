package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.InstaladorBD;
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
        assertSentencias("sql/cu1_usuarios.sql", 6);
        assertSentencias("sql/cu4_investigacion.sql", 7);
        assertSentencias("sql/cu7_contacto.sql", 3);
    }

    private static void assertSentencias(String script, int cantidad) throws IOException {
        List<String> sentencias = InstaladorBD.dividir(InstaladorBD.leerScript(script));
        assertEquals(cantidad, sentencias.size(), script);
        for (String sentencia : sentencias) {
            assertTrue(sentencia.startsWith("CREATE") || sentencia.startsWith("INSERT"), sentencia);
        }
    }

    @Test
    void elNombreDelRolSeNormaliza() throws ErrorNegocio {
        assertEquals("EDITOR_DE_PRENSA", UsuariosNegocio.normalizarRol("  editor de prensa "));
        assertEquals("ATENCION_AL_PUBLICO", UsuariosNegocio.normalizarRol("Atención al público"));
        assertEquals("EDITOR_2", UsuariosNegocio.normalizarRol("editor-2"));
        assertThrows(ErrorNegocio.class, () -> UsuariosNegocio.normalizarRol("ab"));
        assertThrows(ErrorNegocio.class, () -> UsuariosNegocio.normalizarRol("rol;DROP"));
    }

    @Test
    void losCasosDeUsoDelRolSeValidan() throws ErrorNegocio {
        assertEquals(List.of("CU2", "CU4"), UsuariosNegocio.leerCasosDeUso("cu2, CU4 ,cu2"));
        assertEquals(List.of("*"), UsuariosNegocio.leerCasosDeUso(" * "));
        assertThrows(ErrorNegocio.class, () -> UsuariosNegocio.leerCasosDeUso("CU2,CU9"));
        assertThrows(ErrorNegocio.class, () -> UsuariosNegocio.leerCasosDeUso(" , "));
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
