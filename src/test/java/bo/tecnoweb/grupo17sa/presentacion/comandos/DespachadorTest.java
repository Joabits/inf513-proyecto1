package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.presentacion.vista.PlantillaHTML;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Casos que se resuelven antes de llegar a la base de datos (no necesitan PostgreSQL). */
class DespachadorTest {

    private final Despachador despachador = new Despachador();

    @Test
    void ayudaListaLosComandos() {
        Respuesta r = despachador.procesar("visitante@x.com", "HELP");
        assertTrue(r.esExito());
        // Los módulos van en orden de caso de uso (CU1 primero): se busca en todos los bloques, no en el primero
        assertTrue(r.bloques().stream().anyMatch(b -> b.texto().contains("INSBAN[\"titulo\",\"subtitulo\",\"imagen_url\",\"enlace\",\"orden\"]")));
        assertFalse(despachador.procesar("visitante@x.com", "HELP[\"CU9\"]").esExito());
    }

    @Test
    void comandoDesconocido() {
        Respuesta r = despachador.procesar("visitante@x.com", "LISPER[\"*\"]");
        assertFalse(r.esExito());
        assertEquals("Comando desconocido", r.titulo());
        assertEquals("Error: LISPER - Comando desconocido", PlantillaHTML.asunto(r));
    }

    @Test
    void cantidadExactaDeParametros() {
        Respuesta r = despachador.procesar("visitante@x.com", "INSRED[\"TikTok\",\"https://tiktok.com\"]");
        assertFalse(r.esExito());
        assertTrue(r.mensaje().contains("requiere 3 parámetro(s) y se recibieron 2"));
    }

    @Test
    void tipoDeDatoIncorrecto() {
        Respuesta r = despachador.procesar("visitante@x.com", "INSRED[\"TikTok\",\"https://tiktok.com\",\"cuatro\"]");
        assertFalse(r.esExito());
        assertTrue(r.mensaje().contains("El parámetro 3 (orden) debe ser un número entero"));
    }

    @Test
    void sinPermisoParaComandosDeAdministracion() {
        Respuesta r = despachador.procesar("visitante@x.com", "ELIBAN[\"1\"]");
        assertFalse(r.esExito());
        assertEquals("Permiso denegado", r.titulo());
    }

    @Test
    void opcionFueraDeLaLista() {
        Respuesta r = despachador.procesar("empresa@x.com", "SOLINN[\"COMPRA\",\"Tienda\",\"\",\"\",\"Hola\"]");
        assertFalse(r.esExito());
        assertTrue(r.mensaje().contains("debe ser uno de estos valores: SERVICIO, POSTULACION"));
    }

    @Test
    void anioDeTitulacionNoNumerico() {
        Respuesta r = despachador.procesar("grad@x.com",
                "REGGRA[\"Ana\",\"Paz\",\"\",\"dos mil\",\"\",\"\",\"SIN_EMPLEO\",\"\"]");
        assertFalse(r.esExito());
        assertTrue(r.mensaje().contains("El parámetro 4 (anio_titulacion) debe ser un número entero"));
    }

    @Test
    void asuntoMalEscrito() {
        Respuesta r = despachador.procesar("visitante@x.com", "LISBAN[\"*\"");
        assertFalse(r.esExito());
        assertEquals("Error: Comando mal escrito", PlantillaHTML.asunto(r));
    }

    @Test
    void ayudaIncluyeComandosCU3() {
        Respuesta r = despachador.procesar("visitante@x.com", "HELP[\"CU3\"]");
        assertTrue(r.esExito());
        String texto = r.bloques().get(0).texto();
        assertTrue(texto.contains("LISINF[\"filtro\"]"));
        assertTrue(texto.contains("LISMAT[\"filtro\"]"));
        assertTrue(texto.contains("LISDOC[\"filtro\"]"));
        assertTrue(texto.contains("INSMAT[\"sigla\",\"nombre\",\"semestre\",\"creditos\",\"prerrequisito\"]"));
    }

    @Test
    void sinPermisoParaComandosAdministrativosCU3() {
        Respuesta r = despachador.procesar("visitante@x.com", "INSMAT[\"ADM100\",\"Administracion\",\"1\",\"4\",\"\"]");
        assertFalse(r.esExito());
        assertEquals("Permiso denegado", r.titulo());
    }

    @Test
    void parametrosIncorrectosEnCU3() {
        Respuesta r1 = despachador.procesar("visitante@x.com", "INSMAT[\"ADM100\",\"Administracion\"]");
        assertFalse(r1.esExito());
        assertTrue(r1.mensaje().contains("requiere 5 parámetro(s) y se recibieron 2"));

        Respuesta r2 = despachador.procesar("visitante@x.com", "INSMAT[\"ADM100\",\"Administracion\",\"primero\",\"4\",\"\"]");
        assertFalse(r2.esExito());
        assertTrue(r2.mensaje().contains("El parámetro 3 (semestre) debe ser un número entero"));

        Respuesta r3 = despachador.procesar("visitante@x.com", "INSINF[\"Titulo\",\"Contenido\",\"cinco\"]");
        assertFalse(r3.esExito());
        assertTrue(r3.mensaje().contains("El parámetro 3 (orden) debe ser un número entero"));
    }

    @Test
    void ayudaIncluyeComandosCU6SinColisiones() {
        Respuesta r = despachador.procesar("visitante@x.com", "HELP[\"CU6\"]");
        assertTrue(r.esExito());
        String texto = r.bloques().get(0).texto();
        assertTrue(texto.contains("LISNOT[\"filtro\"]"));
        assertTrue(texto.contains("VERNOT[\"id\"]"));
        assertTrue(texto.contains("LISCOM[\"filtro\"]"));
        assertTrue(texto.contains("LISEVT[\"filtro\"]"));
        assertTrue(texto.contains("LISGAL[\"filtro\"]"));
        assertTrue(texto.contains("INSNOT[\"titulo\",\"resumen\",\"contenido\",\"imagen_url\",\"autor\",\"orden\"]"));
        assertTrue(texto.contains("INSCOM[\"titulo\",\"contenido\",\"prioridad\",\"fecha_expiracion\"]"));
        assertTrue(texto.contains("INSEVT[\"titulo\",\"descripcion\",\"fecha_inicio\",\"fecha_fin\",\"lugar\",\"imagen_url\"]"));
        assertTrue(texto.contains("INSGAL[\"titulo\",\"descripcion\",\"imagen_url\",\"evento_id\",\"orden\"]"));
    }
}
