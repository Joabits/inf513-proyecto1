package bo.tecnoweb.grupo17sa.presentacion.correo;

import bo.tecnoweb.grupo17sa.presentacion.vista.ArchivoAdjunto;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorreoTest {

    @Test
    void decodificaAsuntoBase64YQuotedPrintable() {
        String base64 = Base64.getEncoder().encodeToString("MODINT[\"misión\"]".getBytes(StandardCharsets.UTF_8));
        assertEquals("MODINT[\"misión\"]", CodificacionMIME.decodificarEncabezado("=?UTF-8?B?" + base64 + "?="));
        assertEquals("INSBAN[\"Práctica\"]",
                CodificacionMIME.decodificarEncabezado("=?utf-8?Q?INSBAN=5B=22Pr=C3=A1ctica=22=5D?="));
    }

    @Test
    void unePalabrasCodificadasSeguidasYTextoUtf8SinCodificar() {
        assertEquals("LISBANtodo", CodificacionMIME.decodificarEncabezado("=?UTF-8?Q?LISBAN?= =?UTF-8?Q?todo?="));
        String crudo = new String("VERINT[\"Misión\"]".getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        assertEquals("VERINT[\"Misión\"]", CodificacionMIME.decodificarEncabezado(crudo));
    }

    @Test
    void codificarYDecodificarDevuelveElMismoTexto() {
        String asunto = "Éxito: INSBAN - Registro insertado correctamente en la sección de información institucional";
        String codificado = CodificacionMIME.codificarEncabezado(asunto);
        assertTrue(codificado.startsWith("=?UTF-8?B?"));
        assertEquals(asunto, CodificacionMIME.decodificarEncabezado(codificado));
        assertEquals("HELP", CodificacionMIME.codificarEncabezado("HELP"));
    }

    @Test
    void extraeLaDireccionDelRemitente() {
        assertEquals("juan@gmail.com", CodificacionMIME.extraerDireccion("Juan Pérez <Juan@Gmail.com>"));
        assertEquals("juan@gmail.com", CodificacionMIME.extraerDireccion("juan@gmail.com"));
    }

    @Test
    void interpretaLosEncabezadosDeUnCorreoPop3() {
        List<String> lineas = List.of(
                "Received: from mail.example.com",
                "From: =?UTF-8?Q?Daniel_Pe=C3=B1a?= <daniel@gmail.com>",
                "Subject: =?UTF-8?B?SU5TQkFOWyJUw610dWxvIiwi?=",
                " =?UTF-8?B?IiwiaHR0cHM6Ly94LmJvL2EuanBnIiwiIiwiMSJd?=",
                "Message-ID: <abc@mail.gmail.com>",
                "",
                "cuerpo del correo");
        Correo correo = LectorCorreo.interpretar(1, lineas);
        assertEquals("daniel@gmail.com", correo.remitente());
        assertEquals("INSBAN[\"Título\",\"\",\"https://x.bo/a.jpg\",\"\",\"1\"]", correo.asunto());
        assertEquals("<abc@mail.gmail.com>", correo.messageId());
        assertFalse(correo.automatico());
    }

    @Test
    void conAdjuntoArmaUnMensajeMultipart() {
        ArchivoAdjunto csv = new ArchivoAdjunto("graduados.csv", "text/csv; charset=UTF-8",
                "ID;Nombre\r\n1;Andrea\r\n".getBytes(StandardCharsets.UTF_8));
        String mensaje = new EmisorCorreo().armarMensaje("a@b.bo", "Éxito: EXPGRA", "<p>hola</p>", "<x@y>", List.of(csv));
        assertTrue(mensaje.contains("Content-Type: multipart/mixed; boundary="));
        assertTrue(mensaje.contains("Content-Disposition: attachment; filename=\"graduados.csv\""));
        assertTrue(mensaje.contains("In-Reply-To: <x@y>"));
        assertTrue(mensaje.trim().endsWith("--"));
        String sinAdjunto = new EmisorCorreo().armarMensaje("a@b.bo", "Éxito: HELP", "<p>hola</p>", null, List.of());
        assertTrue(sinAdjunto.contains("Content-Type: text/html; charset=UTF-8"));
        assertFalse(sinAdjunto.contains("multipart"));
    }

    @Test
    void reconoceRespuestasAutomaticas() {
        assertTrue(LectorCorreo.interpretar(1, List.of("From: grupo17sa@tecnoweb.org.bo",
                "Subject: Éxito", EmisorCorreo.ENCABEZADO_PROPIO + ": si", "")).automatico());
        assertTrue(LectorCorreo.interpretar(2, List.of("From: MAILER-DAEMON@tecnoweb.org.bo",
                "Subject: Undelivered", "")).automatico());
    }
}
