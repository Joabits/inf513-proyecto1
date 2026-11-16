package bo.tecnoweb.grupo17sa.presentacion.vista;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Resultado de un comando: Éxito o Error, un mensaje y, si corresponde, tablas o párrafos con los datos. */
public final class Respuesta {

    private final boolean exito;
    private final String titulo;
    private final String mensaje;
    private final List<Bloque> bloques = new ArrayList<>();
    private final List<ArchivoAdjunto> adjuntos = new ArrayList<>();
    private final List<Aviso> avisos = new ArrayList<>();
    private boolean aviso;
    private String comando = "";

    /** Correo adicional para otra persona (por ejemplo, avisar al solicitante que su solicitud fue atendida). */
    public record Aviso(String destinatario, Respuesta respuesta) {
    }

    private Respuesta(boolean exito, String titulo, String mensaje) {
        this.exito = exito;
        this.titulo = Objects.toString(titulo, "");
        this.mensaje = Objects.toString(mensaje, "");
    }

    public static Respuesta exito(String titulo, String mensaje) {
        return new Respuesta(true, titulo, mensaje);
    }

    public static Respuesta error(String titulo, String mensaje) {
        return new Respuesta(false, titulo, mensaje);
    }

    /** Mensaje informativo que el sistema envía por su cuenta (no responde a un comando). */
    public static Respuesta aviso(String titulo, String mensaje) {
        Respuesta respuesta = new Respuesta(true, titulo, mensaje);
        respuesta.aviso = true;
        return respuesta;
    }

    public Respuesta avisar(String destinatario, Respuesta mensaje) {
        avisos.add(new Aviso(destinatario, mensaje));
        return this;
    }

    public List<Aviso> avisos() {
        return avisos;
    }

    public boolean esAviso() {
        return aviso;
    }

    public Respuesta agregar(Bloque bloque) {
        bloques.add(bloque);
        return this;
    }

    public Respuesta adjuntar(ArchivoAdjunto archivo) {
        adjuntos.add(archivo);
        return this;
    }

    public List<ArchivoAdjunto> adjuntos() {
        return adjuntos;
    }

    /** Nombre del comando que se respondió (lo pone el Despachador; vacío si el asunto no se pudo leer). */
    public Respuesta conComando(String nombre) {
        this.comando = Objects.toString(nombre, "");
        return this;
    }

    public boolean esExito() {
        return exito;
    }

    public String titulo() {
        return titulo;
    }

    public String mensaje() {
        return mensaje;
    }

    public List<Bloque> bloques() {
        return bloques;
    }

    public String comando() {
        return comando;
    }
}
