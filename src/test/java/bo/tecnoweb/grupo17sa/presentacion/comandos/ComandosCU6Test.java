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

class ComandosCU6Test {

    private final ComandosCU6 modulo = new ComandosCU6();
    private final Despachador despachador = new Despachador();

    @Test
    void metadatosDelModulo() {
        assertEquals("CU6", modulo.codigo());
        assertTrue(modulo.nombre().contains("Noticias"));
        assertEquals(17, modulo.comandos().size());
    }

    @Test
    void clasificacionDeComandosPublicosYPrivados() {
        Map<String, Boolean> comandos = modulo.comandos().stream()
                .collect(Collectors.toMap(DefinicionComando::nombre, DefinicionComando::esPublico));

        // Públicos
        assertTrue(comandos.get("LISNOT"));
        assertTrue(comandos.get("VERNOT"));
        assertTrue(comandos.get("LISCOM"));
        assertTrue(comandos.get("LISEVT"));
        assertTrue(comandos.get("LISGAL"));

        // Administrativos (privados)
        assertFalse(comandos.get("INSNOT"));
        assertFalse(comandos.get("MODNOT"));
        assertFalse(comandos.get("ELINOT"));

        assertFalse(comandos.get("INSCOM"));
        assertFalse(comandos.get("MODCOM"));
        assertFalse(comandos.get("ELICOM"));

        assertFalse(comandos.get("INSEVT"));
        assertFalse(comandos.get("MODEVT"));
        assertFalse(comandos.get("ELIEVT"));

        assertFalse(comandos.get("INSGAL"));
        assertFalse(comandos.get("MODGAL"));
        assertFalse(comandos.get("ELIGAL"));
    }

    @Test
    void validacionDeParametrosEnComandosCU6() {
        // INSNOT: ["titulo","resumen","contenido","imagen_url","autor","orden"] -> 6 params
        Respuesta r1 = despachador.procesar("admin@x.com", "INSNOT[\"T\",\"R\"]");
        assertFalse(r1.esExito());
        assertTrue(r1.mensaje().contains("requiere 6 parámetro(s)"));

        // MODNOT: ["id","titulo","resumen","contenido","imagen_url","autor","orden"] -> 7 params
        Respuesta r2 = despachador.procesar("admin@x.com", "MODNOT[\"uno\",\"T\",\"R\",\"C\",\"\",\"Autor\",\"1\"]");
        assertFalse(r2.esExito());
        assertTrue(r2.mensaje().contains("El parámetro 1 (id) debe ser un número entero"));

        // ELINOT: ["id"] -> 1 param
        Respuesta r3 = despachador.procesar("admin@x.com", "ELINOT[\"no_num\"]");
        assertFalse(r3.esExito());
        assertTrue(r3.mensaje().contains("El parámetro 1 (id) debe ser un número entero"));

        // INSCOM: ["titulo","contenido","prioridad","fecha_expiracion"] -> 4 params
        Respuesta r4 = despachador.procesar("admin@x.com", "INSCOM[\"T\",\"C\",\"INVALIDA\",\"\"]");
        assertFalse(r4.esExito());
        assertTrue(r4.mensaje().contains("debe ser uno de estos valores: BAJA, MEDIA, ALTA, URGENTE"));

        // INSEVT: ["titulo","descripcion","fecha_inicio","fecha_fin","lugar","imagen_url"] -> 6 params
        Respuesta r5 = despachador.procesar("admin@x.com", "INSEVT[\"T\",\"D\",\"fecha_mala\",\"\",\"L\",\"\"]");
        assertFalse(r5.esExito());
        assertTrue(r5.mensaje().contains("debe ser una fecha válida"));

        // MODEVT: ["id","titulo","descripcion","fecha_inicio","fecha_fin","lugar","imagen_url"] -> 7 params
        Respuesta r6 = despachador.procesar("admin@x.com", "MODEVT[\"id_malo\",\"T\",\"D\",\"2026-11-25 18:00\",\"\",\"L\",\"\"]");
        assertFalse(r6.esExito());
        assertTrue(r6.mensaje().contains("El parámetro 1 (id) debe ser un número entero"));

        // INSGAL: ["titulo","descripcion","imagen_url","evento_id","orden"] -> 5 params
        Respuesta r7 = despachador.procesar("admin@x.com", "INSGAL[\"T\",\"D\",\"https://img.com\",\"no_num\",\"1\"]");
        assertFalse(r7.esExito());
        assertTrue(r7.mensaje().contains("El parámetro 4 (evento_id) debe ser un número entero"));
    }

    @Test
    void permisosDenegadosParaNoAdministradores() {
        List<String> comandosPrivados = List.of(
                "INSNOT[\"T\",\"R\",\"C\",\"\",\"A\",\"1\"]",
                "MODNOT[\"1\",\"T\",\"R\",\"C\",\"\",\"A\",\"1\"]",
                "ELINOT[\"1\"]",
                "INSCOM[\"T\",\"C\",\"ALTA\",\"\"]",
                "MODCOM[\"1\",\"T\",\"C\",\"ALTA\",\"\"]",
                "ELICOM[\"1\"]",
                "INSEVT[\"T\",\"D\",\"2026-11-25 18:00\",\"\",\"L\",\"\"]",
                "MODEVT[\"1\",\"T\",\"D\",\"2026-11-25 18:00\",\"\",\"L\",\"\"]",
                "ELIEVT[\"1\"]",
                "INSGAL[\"T\",\"D\",\"https://img.com\",\"\",\"1\"]",
                "MODGAL[\"1\",\"T\",\"D\",\"https://img.com\",\"\",\"1\"]",
                "ELIGAL[\"1\"]"
        );

        for (String c : comandosPrivados) {
            Respuesta r = despachador.procesar("visitante@x.com", c);
            assertFalse(r.esExito(), "El comando " + c + " no debió ejecutarse");
            assertEquals("Permiso denegado", r.titulo());
        }
    }

    @Test
    void insgalConEventoIdOpcional() {
        // Valida que evento_id acepta cadena vacía ""
        Parametro paramEventoId = Parametro.enteroOpcional("evento_id");
        assertNotNull(paramEventoId);
        org.junit.jupiter.api.Assertions.assertNull(paramEventoId.validar(""));
        org.junit.jupiter.api.Assertions.assertNull(paramEventoId.validar("5"));
        assertNotNull(paramEventoId.validar("cinco"));

        // Comando con evento_id vacío ""
        Respuesta r = despachador.procesar("visitante@x.com", "INSGAL[\"Foto\",\"Desc\",\"https://img.com\",\"\",\"1\"]");
        assertFalse(r.esExito());
        // Falla por permiso, no por validación de parámetros
        assertEquals("Permiso denegado", r.titulo());
    }

    @Test
    void formatoDeFechasValidoConHoraYSinHora() {
        Parametro fechaInicio = Parametro.fecha("fecha_inicio");
        org.junit.jupiter.api.Assertions.assertNull(fechaInicio.validar("2026-11-25"));
        org.junit.jupiter.api.Assertions.assertNull(fechaInicio.validar("2026-11-25 18:00"));
        org.junit.jupiter.api.Assertions.assertNull(fechaInicio.validar("25/11/2026 18:00"));
        assertNotNull(fechaInicio.validar("2026-02-30"));
    }
}
