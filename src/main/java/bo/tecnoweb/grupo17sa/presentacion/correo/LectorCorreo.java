package bo.tecnoweb.grupo17sa.presentacion.correo;

import bo.tecnoweb.grupo17sa.Configuracion;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cliente POP3 con sockets (puerto 110), basado en ClientePOP de la Lección 3.0:
 * USER, PASS, STAT, RETR n, DELE n y QUIT. Lee el buzón grupo17sa@tecnoweb.org.bo.
 */
public class LectorCorreo {

    private final String servidor = Configuracion.texto("pop3.servidor");
    private final int puerto = Configuracion.entero("pop3.puerto", 110);
    private final String usuario = Configuracion.texto("pop3.usuario");
    private final String clave = Configuracion.texto("pop3.clave");

    /** Lee todos los correos del buzón y los marca para borrar (DELE), así no se procesan dos veces. */
    public List<Correo> leerCorreos() throws IOException {
        List<Correo> correos = new ArrayList<>();
        try (Socket socket = conectar()) {
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
            DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
            iniciarSesion(entrada, salida);
            int cantidad = contar(entrada, salida);
            for (int i = 1; i <= cantidad; i++) {
                enviar(salida, "RETR " + i);
                esperarOk(entrada.readLine(), "RETR " + i);
                correos.add(interpretar(i, getMultiline(entrada)));
                enviar(salida, "DELE " + i);
                esperarOk(entrada.readLine(), "DELE " + i);
            }
            enviar(salida, "QUIT");   // al cerrar la sesión el servidor borra los mensajes marcados
            entrada.readLine();
        }
        return correos;
    }

    /** Inicia sesión y cuenta los correos, sin leer ni borrar nada (opción --probar-correo). */
    public String probarConexion() throws IOException {
        try (Socket socket = conectar()) {
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
            DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
            iniciarSesion(entrada, salida);
            int cantidad = contar(entrada, salida);
            enviar(salida, "QUIT");
            entrada.readLine();
            return "conectado a " + servidor + ":" + puerto + " como " + usuario + "; correos en el buzón: " + cantidad;
        }
    }

    private Socket conectar() throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(servidor, puerto), 15000);
        } catch (UnknownHostException e) {
            socket.close();
            throw new IOException("No se encontró el servidor " + servidor + " (el DNS no lo resuelve: el servidor está caído o no hay Internet).");
        }
        socket.setSoTimeout(60000);
        return socket;
    }

    private void iniciarSesion(BufferedReader entrada, DataOutputStream salida) throws IOException {
        esperarOk(entrada.readLine(), "el saludo del servidor");
        enviar(salida, "USER " + usuario);
        esperarOk(entrada.readLine(), "USER");
        enviar(salida, "PASS " + clave);
        esperarOk(entrada.readLine(), "PASS (revise pop3.usuario y pop3.clave)");
    }

    private static int contar(BufferedReader entrada, DataOutputStream salida) throws IOException {
        enviar(salida, "STAT");
        String respuesta = entrada.readLine();
        esperarOk(respuesta, "STAT");
        return Integer.parseInt(respuesta.split("\\s+")[1]);
    }

    private static void enviar(DataOutputStream salida, String comando) throws IOException {
        salida.writeBytes(comando + "\r\n");
        salida.flush();
    }

    private static void esperarOk(String linea, String paso) throws IOException {
        if (linea == null) {
            throw new IOException("El servidor POP3 cerró la conexión en " + paso + ".");
        }
        if (!linea.startsWith("+OK")) {
            throw new IOException("El servidor POP3 respondió «" + linea + "» en " + paso + ".");
        }
    }

    /** Lee una respuesta de varias líneas hasta la línea "." (getMultiline de ClientePOP, Lección 3.0). */
    static List<String> getMultiline(BufferedReader in) throws IOException {
        List<String> lineas = new ArrayList<>();
        while (true) {
            String linea = in.readLine();
            if (linea == null) {
                throw new IOException("El servidor cerró la conexión inesperadamente.");
            }
            if (linea.equals(".")) {
                break;
            }
            if (linea.startsWith(".")) {
                linea = linea.substring(1);   // una línea que empieza con "." llega como ".."
            }
            lineas.add(linea);
        }
        return lineas;
    }

    /** Toma From, Subject y Message-ID de los encabezados; une las líneas plegadas (las que empiezan con espacio). */
    static Correo interpretar(int numero, List<String> lineas) {
        Map<String, String> encabezados = new HashMap<>();
        String actual = null;
        for (String linea : lineas) {
            if (linea.isEmpty()) {
                break;   // fin de los encabezados; el cuerpo no se necesita
            }
            if (linea.charAt(0) == ' ' || linea.charAt(0) == '\t') {
                if (actual != null) {
                    encabezados.put(actual, encabezados.get(actual) + linea);
                }
                continue;
            }
            actual = null;
            int dosPuntos = linea.indexOf(':');
            if (dosPuntos <= 0) {
                continue;
            }
            String nombre = linea.substring(0, dosPuntos).trim().toLowerCase(Locale.ROOT);
            if (!encabezados.containsKey(nombre)) {
                encabezados.put(nombre, linea.substring(dosPuntos + 1).trim());
                actual = nombre;
            }
        }
        String remitente = CodificacionMIME.extraerDireccion(
                CodificacionMIME.decodificarEncabezado(encabezados.getOrDefault("from", "")));
        String asunto = CodificacionMIME.decodificarEncabezado(encabezados.getOrDefault("subject", "")).trim();
        String messageId = encabezados.getOrDefault("message-id", "").trim();
        boolean automatico = encabezados.containsKey(EmisorCorreo.ENCABEZADO_PROPIO.toLowerCase(Locale.ROOT))
                || encabezados.getOrDefault("auto-submitted", "no").trim().toLowerCase(Locale.ROOT).startsWith("auto")
                || remitente.startsWith("mailer-daemon") || remitente.startsWith("postmaster");
        return new Correo(numero, remitente, asunto, messageId, automatico);
    }
}
