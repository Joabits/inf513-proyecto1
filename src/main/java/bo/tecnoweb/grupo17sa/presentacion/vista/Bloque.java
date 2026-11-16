package bo.tecnoweb.grupo17sa.presentacion.vista;

/** Parte del cuerpo de una respuesta (una tabla o un párrafo), en HTML para el correo y en texto para la consola. */
public interface Bloque {

    String html();

    String texto();
}
