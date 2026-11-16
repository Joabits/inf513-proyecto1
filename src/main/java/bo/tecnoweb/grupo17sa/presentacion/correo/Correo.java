package bo.tecnoweb.grupo17sa.presentacion.correo;

/**
 * Correo leído del buzón: lo necesario para procesar el comando y responder.
 *
 * @param numero     número del mensaje en el buzón POP3
 * @param remitente  dirección del remitente (mail from), en minúsculas
 * @param asunto     asunto ya decodificado (UTF-8)
 * @param messageId  Message-ID, para que la respuesta quede en el mismo hilo
 * @param automatico true si es una respuesta automática (rebote, nuestra propia respuesta...): no se responde
 */
public record Correo(int numero, String remitente, String asunto, String messageId, boolean automatico) {
}
