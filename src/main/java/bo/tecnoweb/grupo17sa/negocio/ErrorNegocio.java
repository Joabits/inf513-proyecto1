package bo.tecnoweb.grupo17sa.negocio;

/** Una regla del negocio no se cumple (por ejemplo, el registro no existe). Se responde como "Error: ...". */
public class ErrorNegocio extends Exception {

    public ErrorNegocio(String mensaje) {
        super(mensaje);
    }
}
