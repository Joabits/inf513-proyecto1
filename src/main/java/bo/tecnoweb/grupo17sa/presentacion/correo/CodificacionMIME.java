package bo.tecnoweb.grupo17sa.presentacion.correo;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Codificación de encabezados de correo (RFC 2047), sin librerías externas.
 * Gmail envía los asuntos con tildes como =?UTF-8?B?...?= (Base64) o =?UTF-8?Q?...?= (Quoted-Printable).
 */
public final class CodificacionMIME {

    private static final Pattern PALABRA_CODIFICADA = Pattern.compile("=\\?([^?\\s]+)\\?([BbQq])\\?([^?\\s]*)\\?=");

    private CodificacionMIME() {
    }

    /** Decodifica un encabezado leído byte a byte (ISO-8859-1): palabras =?...?= y texto UTF-8 sin codificar. */
    public static String decodificarEncabezado(String crudo) {
        if (crudo == null) {
            return "";
        }
        String texto = comoUtf8(crudo);
        Matcher m = PALABRA_CODIFICADA.matcher(texto);
        StringBuilder sb = new StringBuilder();
        int ultimo = 0;
        boolean anteriorCodificada = false;
        while (m.find()) {
            String entre = texto.substring(ultimo, m.start());
            // Los espacios entre dos palabras codificadas seguidas no forman parte del texto
            if (!(anteriorCodificada && entre.isBlank())) {
                sb.append(entre);
            }
            sb.append(decodificarPalabra(m.group(1), m.group(2), m.group(3), m.group()));
            ultimo = m.end();
            anteriorCodificada = true;
        }
        sb.append(texto.substring(ultimo));
        return sb.toString();
    }

    /** Codifica un encabezado con tildes como =?UTF-8?B?...?=; si es ASCII lo deja igual. */
    public static String codificarEncabezado(String texto) {
        if (texto.chars().allMatch(c -> c >= 32 && c < 127)) {
            return texto;
        }
        StringBuilder resultado = new StringBuilder();
        StringBuilder trozo = new StringBuilder();
        int bytes = 0;
        for (int i = 0; i < texto.length(); ) {
            int punto = texto.codePointAt(i);
            String caracter = new String(Character.toChars(punto));
            int largo = caracter.getBytes(StandardCharsets.UTF_8).length;
            if (bytes + largo > 45) {   // 45 bytes = 60 caracteres en Base64: cada palabra queda bajo 75
                agregarPalabra(resultado, trozo);
                trozo.setLength(0);
                bytes = 0;
            }
            trozo.append(caracter);
            bytes += largo;
            i += Character.charCount(punto);
        }
        agregarPalabra(resultado, trozo);
        return resultado.toString();
    }

    /** "Juan Pérez <juan@gmail.com>" → "juan@gmail.com". */
    public static String extraerDireccion(String from) {
        int abre = from.lastIndexOf('<');
        int cierra = from.lastIndexOf('>');
        String direccion = abre >= 0 && cierra > abre ? from.substring(abre + 1, cierra) : from;
        return direccion.trim().toLowerCase(Locale.ROOT);
    }

    private static void agregarPalabra(StringBuilder resultado, StringBuilder trozo) {
        if (trozo.length() == 0) {
            return;
        }
        if (resultado.length() > 0) {
            resultado.append("\r\n ");   // encabezado plegado en varias líneas
        }
        resultado.append("=?UTF-8?B?")
                .append(Base64.getEncoder().encodeToString(trozo.toString().getBytes(StandardCharsets.UTF_8)))
                .append("?=");
    }

    private static String decodificarPalabra(String juego, String codificacion, String datos, String original) {
        try {
            int asterisco = juego.indexOf('*');   // RFC 2231: =?UTF-8*es?...
            Charset charset = Charset.forName(asterisco >= 0 ? juego.substring(0, asterisco) : juego);
            byte[] bytes = codificacion.equalsIgnoreCase("B")
                    ? Base64.getMimeDecoder().decode(datos)
                    : decodificarQ(datos);
            return new String(bytes, charset);
        } catch (IllegalArgumentException e) {   // juego de caracteres desconocido o Base64 inválido
            return original;
        }
    }

    private static byte[] decodificarQ(String datos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        for (int i = 0; i < datos.length(); i++) {
            char c = datos.charAt(i);
            if (c == '_') {
                salida.write(' ');
            } else if (c == '=' && i + 2 < datos.length() && esHex(datos.charAt(i + 1)) && esHex(datos.charAt(i + 2))) {
                salida.write(Integer.parseInt(datos.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                salida.write(c);
            }
        }
        return salida.toByteArray();
    }

    private static boolean esHex(char c) {
        return Character.digit(c, 16) >= 0;
    }

    /** El encabezado se leyó como ISO-8859-1 (un carácter por byte); si esos bytes son UTF-8 válido se reinterpreta. */
    static String comoUtf8(String latin1) {
        for (int i = 0; i < latin1.length(); i++) {
            if (latin1.charAt(i) > 0xFF) {
                return latin1;   // ya es texto Unicode
            }
        }
        byte[] bytes = latin1.getBytes(StandardCharsets.ISO_8859_1);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return latin1;
        }
    }
}
