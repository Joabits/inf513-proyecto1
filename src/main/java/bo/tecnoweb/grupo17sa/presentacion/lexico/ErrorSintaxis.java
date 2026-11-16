package bo.tecnoweb.grupo17sa.presentacion.lexico;

/** El asunto no respeta el formato COMANDO["param1","param2",...]. */
public class ErrorSintaxis extends Exception {

    public ErrorSintaxis(String mensaje) {
        super(mensaje);
    }
}
