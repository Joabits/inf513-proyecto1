package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.Registro;
import bo.tecnoweb.grupo17sa.modelo.LineaAcceso;
import org.junit.jupiter.api.Test;

import java.util.List;

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

        // El detalle de un error de PostgreSQL trae saltos de línea: Registro lo deja en una sola línea.
        String detalle = "Error en la base de datos: ERROR: no existe la relación «noticia»\n  Position: 108";
        String entrada = Registro.unaLinea("x@y.com | LISNOT[\"*\"] | ERROR | " + detalle + " | 2432 ms");
        assertFalse(entrada.contains("\n"));
        LineaAcceso error = RegistroAccesoDAO.interpretar("2026-11-19 15:06:45 | " + entrada);
        assertEquals("LISNOT", error.comando());
        assertFalse(error.exito());
        assertEquals(2432, error.milisegundos());

        // Un access.log anterior tiene esa entrada partida en dos líneas: al leerlo se unen y el error se cuenta.
        List<String> entradas = RegistroAccesoDAO.unirPartidas(List.of(
                "2026-11-19 15:06:45 | x@y.com | LISNOT[\"*\"] | ERROR | Error en la base de datos: ERROR: no existe la relación «noticia»",
                "  Position: 108 | 2432 ms",
                "2026-11-19 15:06:49 | x@y.com | respuesta enviada | Error: LISNOT - Error en la base de datos"));
        assertEquals(2, entradas.size());
        LineaAcceso unida = RegistroAccesoDAO.interpretar(entradas.get(0));
        assertFalse(unida.exito());
        assertEquals(2432, unida.milisegundos());
    }

    @Test
    void ignoraLasLineasQueNoSonDeUnComando() {
        assertNull(RegistroAccesoDAO.interpretar("2026-11-16 18:49:19 | x@y.com | respuesta enviada | Éxito: HELP - Ayuda"));
        assertNull(RegistroAccesoDAO.interpretar("texto cualquiera"));
    }
}
