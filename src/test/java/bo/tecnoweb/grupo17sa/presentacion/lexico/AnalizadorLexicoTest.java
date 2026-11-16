package bo.tecnoweb.grupo17sa.presentacion.lexico;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalizadorLexicoTest {

    private final AnalizadorLexico analizador = new AnalizadorLexico();

    @Test
    void ejemploDelEnunciado() throws ErrorSintaxis {
        Comando c = analizador.analizar("INSPER[\"4715292\",\"Juan Carlos\",\"Perez Seras\",\"Estudiante\","
                + "\"33554433\",\"71055123\",\"juanperez@uagrm.edu.bo\"]");
        assertEquals("INSPER", c.nombre());
        assertEquals(7, c.cantidad());
        assertEquals("Juan Carlos", c.parametro(1));
        assertEquals("juanperez@uagrm.edu.bo", c.parametro(6));
    }

    @Test
    void asteriscoConEspaciosDeMasYMinusculas() throws ErrorSintaxis {
        Comando c = analizador.analizar("   lisban  [  \"*\"   ]   ");
        assertEquals("LISBAN", c.nombre());
        assertEquals(List.of("*"), c.parametros());
    }

    @Test
    void sinParametrosConYSinCorchetes() throws ErrorSintaxis {
        assertEquals(0, analizador.analizar("INICIO").cantidad());
        assertEquals(0, analizador.analizar("INICIO[]").cantidad());
        assertEquals(0, analizador.analizar("INICIO [ ]").cantidad());
    }

    @Test
    void comillasEscapadasYComasDentroDeUnParametro() throws ErrorSintaxis {
        Comando c = analizador.analizar("INSDES[\"El \\\"mejor\\\" curso\",\"uno, dos\",\"barra \\\\ final\"]");
        assertEquals("El \"mejor\" curso", c.parametro(0));
        assertEquals("uno, dos", c.parametro(1));
        assertEquals("barra \\ final", c.parametro(2));
    }

    @Test
    void tildesYEspaciosDentroDeLasComillas() throws ErrorSintaxis {
        Comando c = analizador.analizar("MODINT[\"mision\",\"  Misión  \",\"Formación en gestión, ñandú\"]");
        assertEquals("Misión", c.parametro(1));
        assertEquals("Formación en gestión, ñandú", c.parametro(2));
    }

    @Test
    void parametroVacioParaLosOpcionales() throws ErrorSintaxis {
        Comando c = analizador.analizar("INSRED[\"TikTok\",\"\",\"4\"]");
        assertEquals("", c.parametro(1));
        assertEquals(null, c.texto(1));
    }

    @Test
    void ignoraPrefijosDeRespuesta() throws ErrorSintaxis {
        assertEquals("HELP", analizador.analizar("Re: RE: Fwd: help").nombre());
    }

    @Test
    void aceptaComillasTipograficasDelCelular() throws ErrorSintaxis {
        assertEquals(List.of("mision"), analizador.analizar("VERINT[“mision”]").parametros());
    }

    @Test
    void erroresDeSintaxisConMensajeClaro() {
        assertTrue(error("").contains("vacío"));
        assertTrue(error("LISBAN[\"*\"").contains("corchete de cierre"));
        assertTrue(error("LISBAN[*]").contains("entre comillas"));
        assertTrue(error("INSBAN[\"a\" \"b\"]").contains("',' o ']'"));
        assertTrue(error("INSBAN[\"a]").contains("comilla de cierre"));
        assertTrue(error("INSBAN[\"a\",]").contains("Sobra una coma"));
        assertTrue(error("LISBAN[\"*\"] hola").contains("texto de más"));
        assertTrue(error("123ABC").contains("empezar con una letra"));
        assertTrue(error("LISBAN(\"*\")").contains("se esperaba '['"));
        assertTrue(error("[\"*\"]").contains("nombre del comando"));
    }

    private String error(String asunto) {
        return assertThrows(ErrorSintaxis.class, () -> analizador.analizar(asunto)).getMessage();
    }
}
