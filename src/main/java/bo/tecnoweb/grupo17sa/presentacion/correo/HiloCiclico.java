package bo.tecnoweb.grupo17sa.presentacion.correo;

import bo.tecnoweb.grupo17sa.Configuracion;
import bo.tecnoweb.grupo17sa.Registro;
import bo.tecnoweb.grupo17sa.presentacion.comandos.Despachador;
import bo.tecnoweb.grupo17sa.presentacion.vista.PlantillaHTML;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Demonio del sistema: cada pop3.intervalo segundos lee el buzón por POP3,
 * procesa el comando de cada correo y responde al remitente por SMTP.
 */
public class HiloCiclico extends Thread {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LectorCorreo lector = new LectorCorreo();
    private final EmisorCorreo emisor = new EmisorCorreo();
    private final Despachador despachador = new Despachador();
    private final int intervalo = Math.max(5, Configuracion.entero("pop3.intervalo", 10));

    public HiloCiclico() {
        super("hilo-correo");
    }

    public int intervaloSegundos() {
        return intervalo;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            revisarBuzon();
            try {
                Thread.sleep(intervalo * 1000L);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /** Un ciclo: leer el buzón, procesar cada correo y responder. */
    public void revisarBuzon() {
        List<Correo> correos;
        try {
            correos = lector.leerCorreos();
        } catch (IOException e) {
            Registro.error("No se pudo leer el buzón POP3", e);
            System.out.println(hora() + " No se pudo leer el buzón: " + e.getMessage());
            return;
        }
        if (!correos.isEmpty()) {
            System.out.println(hora() + " Correos nuevos: " + correos.size());
        }
        for (Correo correo : correos) {
            atender(correo);
        }
    }

    private void atender(Correo correo) {
        if (correo.automatico() || correo.remitente().isEmpty()) {
            Registro.acceso(correo.remitente() + " | " + correo.asunto() + " | IGNORADO | respuesta automática o sin remitente");
            return;
        }
        System.out.println(hora() + " De " + correo.remitente() + ": " + correo.asunto());
        Respuesta respuesta = despachador.procesar(correo.remitente(), correo.asunto());
        String asunto = PlantillaHTML.asunto(respuesta);
        try {
            emisor.enviar(correo.remitente(), asunto, PlantillaHTML.html(respuesta, correo.asunto()), correo.messageId(),
                    respuesta.adjuntos());
            Registro.acceso(correo.remitente() + " | respuesta enviada | " + asunto);
            System.out.println(hora() + "   Respuesta enviada: " + asunto);
        } catch (IOException e) {
            Registro.error("No se pudo enviar la respuesta a " + correo.remitente() + " (" + asunto + ")", e);
            System.out.println(hora() + "   No se pudo enviar la respuesta: " + e.getMessage());
        }
        for (Respuesta.Aviso aviso : respuesta.avisos()) {
            enviarAviso(aviso, correo.asunto());
        }
    }

    /** Correo adicional para otra persona, por ejemplo el solicitante de INNOVA cuando se atiende su solicitud. */
    private void enviarAviso(Respuesta.Aviso aviso, String comandoOrigen) {
        String asunto = PlantillaHTML.asunto(aviso.respuesta());
        try {
            emisor.enviar(aviso.destinatario(), asunto, PlantillaHTML.html(aviso.respuesta(), comandoOrigen), null,
                    aviso.respuesta().adjuntos());
            Registro.acceso(aviso.destinatario() + " | aviso enviado | " + asunto);
            System.out.println(hora() + "   Aviso enviado a " + aviso.destinatario() + ": " + asunto);
        } catch (IOException e) {
            Registro.error("No se pudo enviar el aviso a " + aviso.destinatario() + " (" + asunto + ")", e);
            System.out.println(hora() + "   No se pudo enviar el aviso a " + aviso.destinatario() + ": " + e.getMessage());
        }
    }

    private static String hora() {
        return LocalTime.now().format(HORA);
    }
}
