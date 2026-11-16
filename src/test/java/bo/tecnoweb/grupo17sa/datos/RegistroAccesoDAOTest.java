package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.LineaAcceso;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroAccesoDAOTest {

    @Test
    void interpretaUnaLineaDeComando() {
        LineaAcceso l = RegistroAccesoDAO.interpretar(
                "2026-11-16 18:49:19 | daniel@gmail.com | Re: lisban[\"*\"] | ÉXITO | Banners | 12 ms");
        assertEquals("LISBAN", l.comando());
        assertEquals("daniel@gmail.com", l.correo());
        assertTrue(l.exito());
        assertEquals(12, l.milisegundos());
    }

    @Test
    void interpretaErroresYAsuntosMalEscritos() {
        LineaAcceso l = RegistroAccesoDAO.interpretar(
                "2026-11-16 18:49:19 | x@y.com | [\"*\"] | ERROR | Comando mal escrito: falta el nombre | 3 ms");
        assertFalse(l.exito());
        assertEquals("(asunto mal escrito)", l.comando());
    }

    @Test
    void ignoraLasLineasQueNoSonDeUnComando() {
        assertNull(RegistroAccesoDAO.interpretar("2026-11-16 18:49:19 | x@y.com | respuesta enviada | Éxito: HELP - Ayuda"));
        assertNull(RegistroAccesoDAO.interpretar("texto cualquiera"));
    }
}
