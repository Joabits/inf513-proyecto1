package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComandosCU3Test {

    private final ComandosCU3 modulo = new ComandosCU3();
    private final Despachador despachador = new Despachador();

    @Test
    void metadatosDelModulo() {
        assertEquals("CU3", modulo.codigo());
        assertTrue(modulo.nombre().contains("Académica"));
        assertEquals(12, modulo.comandos().size());
    }

    @Test
    void clasificacionDeComandosPublicosYPrivados() {
        Map<String, Boolean> comandos = modulo.comandos().stream()
                .collect(Collectors.toMap(DefinicionComando::nombre, DefinicionComando::esPublico));

        // Públicos
        assertTrue(comandos.get("LISINF"));
        assertTrue(comandos.get("LISMAT"));
        assertTrue(comandos.get("LISDOC"));

        // Administrativos (privados)
        assertFalse(comandos.get("INSINF"));
        assertFalse(comandos.get("MODINF"));
        assertFalse(comandos.get("ELIINF"));

        assertFalse(comandos.get("INSMAT"));
        assertFalse(comandos.get("MODMAT"));
        assertFalse(comandos.get("ELIMAT"));

        assertFalse(comandos.get("INSDOC"));
        assertFalse(comandos.get("MODDOC"));
        assertFalse(comandos.get("ELIDOC"));
    }

    @Test
    void validacionDeParametrosEnComandosCU3() {
        // INSINF: ["titulo", "contenido", "orden"] -> 3 params
        Respuesta r1 = despachador.procesar("admin@x.com", "INSINF[\"T\",\"C\"]");
        assertFalse(r1.esExito());
        assertTrue(r1.mensaje().contains("requiere 3 parámetro(s)"));

        // MODINF: ["id", "titulo", "contenido", "orden"] -> 4 params
        Respuesta r2 = despachador.procesar("admin@x.com", "MODINF[\"uno\",\"T\",\"C\",\"1\"]");
        assertFalse(r2.esExito());
        assertTrue(r2.mensaje().contains("El parámetro 1 (id) debe ser un número entero"));

        // ELIINF: ["id"] -> 1 param
        Respuesta r3 = despachador.procesar("admin@x.com", "ELIINF[\"no_num\"]");
        assertFalse(r3.esExito());
        assertTrue(r3.mensaje().contains("El parámetro 1 (id) debe ser un número entero"));

        // INSMAT: ["sigla", "nombre", "semestre", "creditos", "prerrequisito"] -> 5 params
        Respuesta r4 = despachador.procesar("admin@x.com", "INSMAT[\"ADM100\",\"Nombre\",\"sem\",\"4\",\"\"]");
        assertFalse(r4.esExito());
        assertTrue(r4.mensaje().contains("El parámetro 3 (semestre) debe ser un número entero"));

        Respuesta r5 = despachador.procesar("admin@x.com", "INSMAT[\"ADM100\",\"Nombre\",\"1\",\"cred\",\"\"]");
        assertFalse(r5.esExito());
        assertTrue(r5.mensaje().contains("El parámetro 4 (creditos) debe ser un número entero"));

        // MODMAT: ["id", "sigla", "nombre", "semestre", "creditos", "prerrequisito"] -> 6 params
        Respuesta r6 = despachador.procesar("admin@x.com", "MODMAT[\"1\",\"ADM100\",\"Nombre\",\"1\",\"4\",\"\"]");
        assertNotNull(r6);

        // INSDOC: ["nombre", "titulo", "cargo", "correo", "materia_id"] -> 5 params
        Respuesta r7 = despachador.procesar("admin@x.com", "INSDOC[\"Docente\",\"Lic\",\"Titular\",\"correo_invalido\",\"1\"]");
        assertFalse(r7.esExito());
        assertTrue(r7.mensaje().contains("El parámetro 4 (correo) debe ser un correo electrónico válido"));

        Respuesta r8 = despachador.procesar("admin@x.com", "INSDOC[\"Docente\",\"Lic\",\"Titular\",\"doc@uagrm.edu.bo\",\"id_invalido\"]");
        assertFalse(r8.esExito());
        assertTrue(r8.mensaje().contains("El parámetro 5 (materia_id) debe ser un número entero"));
    }

    @Test
    void permisosDenegadosParaNoAdministradores() {
        List<String> comandosPrivados = List.of(
                "INSINF[\"T\",\"C\",\"1\"]",
                "MODINF[\"1\",\"T\",\"C\",\"1\"]",
                "ELIINF[\"1\"]",
                "INSMAT[\"ADM100\",\"Nom\",\"1\",\"4\",\"\"]",
                "MODMAT[\"1\",\"ADM100\",\"Nom\",\"1\",\"4\",\"\"]",
                "ELIMAT[\"1\"]",
                "INSDOC[\"Doc\",\"Lic\",\"Tit\",\"d@uagrm.edu.bo\",\"1\"]",
                "MODDOC[\"1\",\"Doc\",\"Lic\",\"Tit\",\"d@uagrm.edu.bo\",\"1\"]",
                "ELIDOC[\"1\"]"
        );

        for (String cmd : comandosPrivados) {
            Respuesta r = despachador.procesar("usuario_comun@correo.com", cmd);
            assertFalse(r.esExito(), "Comando debería fallar por permiso: " + cmd);
            assertEquals("Permiso denegado", r.titulo());
        }
    }
}
