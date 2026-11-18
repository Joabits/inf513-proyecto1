package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lo que el despachador resuelve antes de llegar a la base de datos en los comandos de CU1, CU4 y CU7. */
class ComandosCU1CU4CU7Test {

    private final Despachador despachador = new Despachador();

    private void assertError(String asunto, String titulo, String fragmento) {
        Respuesta r = despachador.procesar("visitante@x.com", asunto);
        assertFalse(r.esExito(), asunto);
        assertEquals(titulo, r.titulo(), asunto);
        assertTrue(r.mensaje().contains(fragmento), asunto + " → " + r.mensaje());
    }

    @Test
    void losComandosDeAdministracionPidenPermiso() {
        assertError("LISUSU[\"*\"]", "Permiso denegado", "CU1");
        assertError("INSPRO[\"Un título de prueba\",\"aula\",\"\",\"Responsable\",\"\",\"2026\",\"EN_CURSO\",\"Resumen\"]",
                "Permiso denegado", "CU4");
        assertError("LISMSJ[\"*\"]", "Permiso denegado", "CU7");
    }

    @Test
    void validaCantidadYTipoDeParametros() {
        assertError("INSUSU[\"a@b.com\",\"Nombre\"]", "Parámetros incorrectos", "requiere 3");
        assertError("INSUSU[\"no-es-correo\",\"Nombre\",\"ADMINISTRADOR\"]", "Parámetros incorrectos", "correo");
        assertError("INSPRO[\"Título\",\"otro\",\"\",\"R\",\"\",\"2026\",\"EN_CURSO\",\"Resumen\"]", "Parámetros incorrectos", "tipo");
        assertError("INSDOC[\"Título\",\"\",\"REGLAMENTO\",\"archivo.pdf\"]", "Parámetros incorrectos", "enlace");
        assertError("PUBREV[\"1\",\"quizás\"]", "Parámetros incorrectos", "publicada");
        assertError("ENVMSJ[\"Ana\",\"\",\"Asunto\"]", "Parámetros incorrectos", "requiere 4");
    }

    @Test
    void lasReglasQueNoNecesitanBaseResponden() {
        assertError("BUSCAR[\"ab\"]", "No se pudo completar BUSCAR", "al menos 3 caracteres");
        assertError("ENVMSJ[\"Ana\",\"\",\"Hola\",\"corto\"]", "No se pudo completar ENVMSJ", "muy corto");
        assertError("ENVMSJ[\"Ana\",\"llámame\",\"Hola\",\"Un mensaje con suficiente largo\"]", "No se pudo completar ENVMSJ",
                "teléfono");
    }

    @Test
    void laAyudaListaLosComandosDeCadaCasoDeUso() {
        for (String cu : new String[] {"CU1", "CU4", "CU7"}) {
            Respuesta r = despachador.procesar("visitante@x.com", "HELP[\"" + cu + "\"]");
            assertTrue(r.esExito(), cu);
        }
        Respuesta cu4 = despachador.procesar("visitante@x.com", "HELP[\"CU4\"]");
        assertTrue(cu4.bloques().stream().anyMatch(b -> b.texto().contains("INSART[\"edicion\",\"titulo\",\"autores\",\"resumen\",\"enlace_pdf\"]")));
    }
}
