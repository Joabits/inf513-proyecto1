package bo.tecnoweb.grupo17sa.presentacion.correo;

import bo.tecnoweb.grupo17sa.Configuracion;
import bo.tecnoweb.grupo17sa.presentacion.vista.ArchivoAdjunto;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Cliente SMTP con sockets, basado en ClienteSMTP de la Lección 3.0:
 * EHLO, MAIL FROM, RCPT TO, DATA y QUIT. Envía la respuesta en HTML (UTF-8, Base64).
 *
 * Con smtp.seguridad=ninguna funciona igual que el ejemplo del docente (puerto 25 de mail.tecnoweb.org.bo).
 * Con starttls (puerto 587) o ssl (puerto 465) cifra el mismo socket y se autentica con AUTH LOGIN,
 * por ejemplo para salir por Gmail cuando el puerto 25 está bloqueado.
 */
public class EmisorCorreo {

    /** Marca las respuestas del sistema para no responderlas si llegan de vuelta al buzón. */
    public static final String ENCABEZADO_PROPIO = "X-Grupo17sa-Respuesta";

    private final String servidor = Configuracion.texto("smtp.servidor");
    private final int puerto = Configuracion.entero("smtp.puerto", 25);
    private final String seguridad = Configuracion.texto("smtp.seguridad").toLowerCase(Locale.ROOT);
    private final String usuario = Configuracion.texto("smtp.usuario");
    private final String clave = Configuracion.texto("smtp.clave");
    private final String remitente = Configuracion.texto("smtp.remitente");

    private Socket socket;
    private BufferedReader entrada;
    private DataOutputStream salida;

    public synchronized void enviar(String destinatario, String asunto, String html, String enRespuestaA,
                                    List<ArchivoAdjunto> adjuntos) throws IOException {
        try {
            abrirSesion();
            comando("MAIL FROM:<" + remitente + ">", "250");
            comando("RCPT TO:<" + destinatario + ">", "250", "251");
            comando("DATA", "354");
            salida.writeBytes(armarMensaje(destinatario, asunto, html, enRespuestaA, adjuntos) + "\r\n.\r\n");
            salida.flush();
            esperar("el fin de DATA", "250");
            comando("QUIT", "221");
        } finally {
            cerrar();
        }
    }

    /** Se conecta y se autentica, sin enviar nada (opción --probar-correo). */
    public synchronized String probarConexion() throws IOException {
        try {
            abrirSesion();
            comando("QUIT", "221");
            return "conectado a " + servidor + ":" + puerto + " (" + (seguridad.isEmpty() ? "ninguna" : seguridad) + ")"
                    + (usuario.isEmpty() ? " sin autenticación" : ", autenticado como " + usuario);
        } finally {
            cerrar();
        }
    }

    private void abrirSesion() throws IOException {
        try {
            if (seguridad.equals("ssl")) {
                socket = SSLSocketFactory.getDefault().createSocket(servidor, puerto);
            } else {
                socket = new Socket();
                socket.connect(new InetSocketAddress(servidor, puerto), 15000);
            }
        } catch (UnknownHostException e) {
            throw new IOException("No se encontró el servidor " + servidor + " (el DNS no lo resuelve: el servidor está caído o no hay Internet).");
        }
        socket.setSoTimeout(60000);
        abrirFlujos();
        esperar("el saludo del servidor", "220");
        comando("EHLO " + dominio(), "250");
        if (seguridad.equals("starttls")) {
            comando("STARTTLS", "220");
            SSLSocketFactory fabrica = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket seguro = (SSLSocket) fabrica.createSocket(socket, servidor, puerto, true);
            seguro.startHandshake();
            socket = seguro;
            abrirFlujos();
            comando("EHLO " + dominio(), "250");
        }
        if (!usuario.isEmpty()) {
            comando("AUTH LOGIN", "334");
            enviarLinea(base64(usuario));
            esperar("AUTH LOGIN (usuario)", "334");
            enviarLinea(base64(clave));   // la contraseña nunca se muestra en los mensajes de error
            esperar("AUTH LOGIN (revise smtp.usuario y smtp.clave)", "235");
        }
    }

    private void abrirFlujos() throws IOException {
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
        salida = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Encabezados y cuerpo del correo. Sin adjuntos: text/html en Base64.
     * Con adjuntos: multipart/mixed con la parte HTML y un parte por archivo.
     */
    String armarMensaje(String destinatario, String asunto, String html, String enRespuestaA, List<ArchivoAdjunto> adjuntos) {
        StringBuilder m = new StringBuilder();
        m.append("Date: ").append(ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("\r\n");
        m.append("From: ").append(remitente).append("\r\n");
        m.append("To: ").append(destinatario).append("\r\n");
        m.append("Subject: ").append(CodificacionMIME.codificarEncabezado(asunto)).append("\r\n");
        m.append("Message-ID: <").append(UUID.randomUUID()).append('@').append(dominio()).append(">\r\n");
        if (enRespuestaA != null && !enRespuestaA.isBlank()) {
            m.append("In-Reply-To: ").append(enRespuestaA).append("\r\n");
            m.append("References: ").append(enRespuestaA).append("\r\n");
        }
        m.append("Auto-Submitted: auto-replied\r\n");
        m.append(ENCABEZADO_PROPIO).append(": si\r\n");
        m.append("MIME-Version: 1.0\r\n");
        String cuerpoHtml = Base64.getMimeEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
        if (adjuntos == null || adjuntos.isEmpty()) {
            m.append("Content-Type: text/html; charset=UTF-8\r\n");
            m.append("Content-Transfer-Encoding: base64\r\n\r\n");
            m.append(cuerpoHtml);
            return m.toString();
        }
        String limite = "----=_grupo17sa_" + UUID.randomUUID().toString().replace("-", "");
        m.append("Content-Type: multipart/mixed; boundary=\"").append(limite).append("\"\r\n\r\n");
        m.append("--").append(limite).append("\r\n");
        m.append("Content-Type: text/html; charset=UTF-8\r\n");
        m.append("Content-Transfer-Encoding: base64\r\n\r\n");
        m.append(cuerpoHtml).append("\r\n");
        for (ArchivoAdjunto adjunto : adjuntos) {
            String nombre = CodificacionMIME.codificarEncabezado(adjunto.nombre());
            m.append("--").append(limite).append("\r\n");
            m.append("Content-Type: ").append(adjunto.tipoMime()).append("; name=\"").append(nombre).append("\"\r\n");
            m.append("Content-Disposition: attachment; filename=\"").append(nombre).append("\"\r\n");
            m.append("Content-Transfer-Encoding: base64\r\n\r\n");
            m.append(Base64.getMimeEncoder().encodeToString(adjunto.contenido())).append("\r\n");
        }
        m.append("--").append(limite).append("--");
        return m.toString();
    }

    private String comando(String texto, String... codigos) throws IOException {
        enviarLinea(texto);
        return esperar(texto, codigos);
    }

    private void enviarLinea(String texto) throws IOException {
        salida.writeBytes(texto + "\r\n");
        salida.flush();
    }

    private String esperar(String paso, String... codigos) throws IOException {
        String respuesta = getMultiline();
        for (String codigo : codigos) {
            if (respuesta.startsWith(codigo)) {
                return respuesta;
            }
        }
        throw new IOException("El servidor SMTP respondió «" + respuesta.replace('\n', ' ') + "» en " + paso + ".");
    }

    /** Lee una respuesta SMTP de una o varias líneas (getMultiline de ClienteSMTP, Lección 3.0). */
    private String getMultiline() throws IOException {
        StringBuilder lineas = new StringBuilder();
        while (true) {
            String linea = entrada.readLine();
            if (linea == null) {
                throw new IOException("El servidor SMTP cerró la conexión.");
            }
            if (lineas.length() > 0) {
                lineas.append('\n');
            }
            lineas.append(linea);
            if (linea.length() < 4 || linea.charAt(3) == ' ') {
                return lineas.toString();   // "250 OK" termina; "250-..." sigue
            }
        }
    }

    private String dominio() {
        int arroba = remitente.indexOf('@');
        return arroba >= 0 ? remitente.substring(arroba + 1) : "localhost";
    }

    private static String base64(String texto) {
        return Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private void cerrar() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // no importa: la sesión ya terminó
        }
        socket = null;
    }
}
