package bo.tecnoweb.grupo17sa.presentacion.comandos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParametroTest {

    @Test
    void entero() {
        assertNull(Parametro.entero("orden").validar("3"));
        assertNull(Parametro.entero("orden").validar("-2"));
        assertNotNull(Parametro.entero("orden").validar("tres"));
        assertNotNull(Parametro.entero("orden").validar("2.5"));
        assertNotNull(Parametro.entero("orden").validar("99999999999"));
        assertNotNull(Parametro.entero("orden").validar(""));
    }

    @Test
    void textoObligatorioUOpcionalYLargoMaximo() {
        assertNotNull(Parametro.texto("titulo", 5).validar(""));
        assertNull(Parametro.textoOpcional("subtitulo", 5).validar(""));
        assertNull(Parametro.texto("titulo", 5).validar("Hola"));
        assertNotNull(Parametro.texto("titulo", 5).validar("Hola mundo"));
    }

    @Test
    void opcionAceptaMinusculasYTildes() {
        Parametro tipo = Parametro.opcion("tipo", "CONVENIO", "PRACTICA", "EN_TRAMITE");
        assertNull(tipo.validar("práctica"));
        assertNull(tipo.validar("En trámite"));
        assertNotNull(tipo.validar("pasantía"));
        org.junit.jupiter.api.Assertions.assertEquals("EN_TRAMITE", Parametro.normalizarOpcion(" en trámite "));
        org.junit.jupiter.api.Assertions.assertEquals("SIN_EMPLEO", Parametro.normalizarOpcion("sin-empleo"));
    }

    @Test
    void enlaceCorreoFechaDecimalYBooleano() {
        assertNull(Parametro.url("url", 500).validar("https://www.uagrm.edu.bo"));
        assertNotNull(Parametro.url("url", 500).validar("www.uagrm.edu.bo"));
        assertNull(Parametro.urlOpcional("enlace", 500).validar(""));
        assertNull(Parametro.correo("correo", 120).validar("juanperez@uagrm.edu.bo"));
        assertNotNull(Parametro.correo("correo", 120).validar("juanperez"));
        assertNull(Parametro.fecha("fecha").validar("2026-11-20"));
        assertNull(Parametro.fecha("fecha").validar("20/11/2026"));
        assertNotNull(Parametro.fecha("fecha").validar("2026-02-30"));
        assertNull(Parametro.decimal("monto").validar("150.50"));
        assertNotNull(Parametro.decimal("monto").validar("ciento"));
        assertNull(Parametro.booleano("activo").validar("si"));
        assertNotNull(Parametro.booleano("activo").validar("tal vez"));
    }
}
