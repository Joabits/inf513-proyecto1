package bo.tecnoweb.grupo17sa.presentacion.lexico;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Analizador léxico del asunto del correo. Formato del enunciado:
 *
 *     COMANDO["param1","param2",...]
 *
 * Reglas:
 *  - Se ignoran los espacios en blanco de más y los prefijos de respuesta (Re:, RE:, RV:, Fwd:).
 *  - El nombre del comando se escribe en mayúsculas o minúsculas (se convierte a mayúsculas).
 *  - Cada parámetro va entre comillas dobles. Dentro de un parámetro, \" es una comilla y \\ una barra.
 *  - Un comando sin parámetros se escribe INICIO o INICIO[].
 *
 * Recorre el texto carácter por carácter (un pequeño autómata) para poder explicar en qué parte
 * está el error cuando el asunto está mal escrito.
 */
public class AnalizadorLexico {

    private static final Pattern PREFIJO_RESPUESTA =
            Pattern.compile("^(?:(?:re|rv|fw|fwd)\\s*:\\s*)+", Pattern.CASE_INSENSITIVE);

    public Comando analizar(String asunto) throws ErrorSintaxis {
        if (asunto == null || asunto.isBlank()) {
            throw new ErrorSintaxis("El asunto del correo está vacío. Escriba un comando, por ejemplo HELP.");
        }
        String texto = PREFIJO_RESPUESTA.matcher(asunto.replace(' ', ' ').strip()).replaceFirst("");
        int n = texto.length();

        // 1. Nombre del comando: letras y dígitos, empezando con una letra
        int pos = 0;
        while (pos < n && Character.isLetterOrDigit(texto.charAt(pos))) {
            pos++;
        }
        if (pos == 0) {
            throw new ErrorSintaxis("El asunto debe empezar con el nombre del comando, por ejemplo LISBAN[\"*\"].");
        }
        String nombre = texto.substring(0, pos).toUpperCase(Locale.ROOT);
        if (!Character.isLetter(nombre.charAt(0))) {
            throw new ErrorSintaxis("El nombre del comando debe empezar con una letra: " + nombre);
        }

        // 2. Sin corchetes: comando sin parámetros
        List<String> parametros = new ArrayList<>();
        pos = saltarEspacios(texto, pos);
        if (pos == n) {
            return new Comando(nombre, parametros);
        }
        if (texto.charAt(pos) != '[') {
            throw new ErrorSintaxis("Después de " + nombre + " se esperaba '[' con los parámetros entre comillas, "
                    + "pero se encontró «" + texto.substring(pos) + "».");
        }

        // 3. Lista de parámetros entre corchetes
        pos = saltarEspacios(texto, pos + 1);
        if (pos < n && texto.charAt(pos) == ']') {
            pos++;
        } else {
            while (true) {
                int numero = parametros.size() + 1;
                if (pos >= n) {
                    throw new ErrorSintaxis("Falta el corchete de cierre ']'.");
                }
                char c = texto.charAt(pos);
                if (c == ']') {
                    throw new ErrorSintaxis("Sobra una coma antes de ']'.");
                }
                if (!esComilla(c)) {
                    throw new ErrorSintaxis("El parámetro " + numero + " debe ir entre comillas dobles, por ejemplo \"valor\".");
                }
                pos++;
                StringBuilder valor = new StringBuilder();
                boolean cerrado = false;
                while (pos < n) {
                    char d = texto.charAt(pos);
                    if (d == '\\' && pos + 1 < n && (esComilla(texto.charAt(pos + 1)) || texto.charAt(pos + 1) == '\\')) {
                        valor.append(texto.charAt(pos + 1));   // comilla o barra escapada
                        pos += 2;
                    } else if (esComilla(d)) {
                        cerrado = true;
                        pos++;
                        break;
                    } else {
                        valor.append(d);
                        pos++;
                    }
                }
                if (!cerrado) {
                    throw new ErrorSintaxis("Falta la comilla de cierre del parámetro " + numero + ".");
                }
                parametros.add(valor.toString().strip());

                pos = saltarEspacios(texto, pos);
                if (pos >= n) {
                    throw new ErrorSintaxis("Falta el corchete de cierre ']'.");
                }
                char separador = texto.charAt(pos);
                if (separador == ',') {
                    pos = saltarEspacios(texto, pos + 1);
                } else if (separador == ']') {
                    pos++;
                    break;
                } else {
                    throw new ErrorSintaxis("Después del parámetro " + numero + " se esperaba ',' o ']', "
                            + "pero se encontró '" + separador + "'. ¿Falta una coma o una comilla?");
                }
            }
        }

        // 4. Nada más después del corchete
        pos = saltarEspacios(texto, pos);
        if (pos < n) {
            throw new ErrorSintaxis("Hay texto de más después de ']': «" + texto.substring(pos) + "».");
        }
        return new Comando(nombre, parametros);
    }

    private static int saltarEspacios(String texto, int pos) {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    /** Comilla doble normal o tipográfica (algunos celulares cambian " por “ ”). */
    private static boolean esComilla(char c) {
        return c == '"' || c == '“' || c == '”' || c == '„';
    }
}
