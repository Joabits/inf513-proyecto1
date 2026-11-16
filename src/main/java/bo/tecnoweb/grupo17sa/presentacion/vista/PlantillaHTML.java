package bo.tecnoweb.grupo17sa.presentacion.vista;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Arma el correo de respuesta en HTML sencillo: encabezado con el nombre de la Carrera, la línea
 * "Éxito:" o "Error:" y las tablas con los datos. Los estilos van en línea porque Gmail no respeta
 * las hojas de estilo. Color institucional: azul #21366E.
 */
public final class PlantillaHTML {

    public static final String AZUL = "#21366E";
    private static final String VERDE = "#1e7b34";
    private static final String ROJO = "#b3261e";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private PlantillaHTML() {
    }

    /** Asunto del correo de respuesta, por ejemplo "Éxito: INSBAN - Registro insertado". */
    public static String asunto(Respuesta r) {
        if (r.esAviso()) {
            return "Aviso: " + r.titulo();
        }
        String estado = r.esExito() ? "Éxito" : "Error";
        String comando = r.comando().isEmpty() ? "" : r.comando() + " - ";
        return estado + ": " + comando + r.titulo();
    }

    public static String html(Respuesta r, String asuntoRecibido) {
        String color = r.esAviso() ? AZUL : r.esExito() ? VERDE : ROJO;
        String estado = r.esAviso() ? "Aviso" : r.esExito() ? "Éxito" : "Error";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'><title>")
                .append(escapar(asunto(r))).append("</title></head>");
        sb.append("<body style='margin:0;padding:16px;background:#ffffff;font-family:Arial,Helvetica,sans-serif;"
                + "font-size:14px;line-height:1.45;color:#222'>");
        sb.append("<div style='max-width:720px'>");

        // Encabezado
        sb.append("<div style='border-bottom:2px solid ").append(AZUL).append(";padding-bottom:6px;margin-bottom:14px'>")
                .append("<div style='font-size:16px;font-weight:bold;color:").append(AZUL)
                .append("'>Carrera de Administración de Empresas - UAGRM</div>")
                .append("<div style='font-size:12px;color:#666'>Sistema vía e-mail · Grupo 17 SA</div></div>");

        // Resultado
        sb.append("<div style='font-size:15px;font-weight:bold;margin-bottom:4px'>").append(escapar(r.titulo())).append("</div>");
        sb.append("<div><b style='color:").append(color).append("'>").append(estado).append(":</b> ")
                .append(escaparConSaltos(r.mensaje())).append("</div>");
        for (Bloque bloque : r.bloques()) {
            sb.append(bloque.html());
        }
        if (!r.adjuntos().isEmpty()) {
            sb.append("<p style='margin:14px 0 0'>Archivo adjunto: ").append(escapar(nombresAdjuntos(r))).append("</p>");
        }

        // Pie
        sb.append("<div style='margin-top:22px;padding-top:6px;border-top:1px solid #ddd;font-size:12px;color:#777'>");
        if (r.esAviso()) {
            sb.append("Mensaje automático del sistema, ").append(LocalDateTime.now().format(FECHA)).append(".");
        } else {
            sb.append("Comando recibido: ").append(escapar(asuntoRecibido)).append(" · ")
                    .append(LocalDateTime.now().format(FECHA)).append(".");
        }
        sb.append(" Envíe HELP para ver la lista de comandos.</div>");
        sb.append("</div></body></html>");
        return sb.toString();
    }

    /** Título de una tabla, párrafo o gráfico dentro del correo. */
    public static String tituloSeccion(String titulo) {
        return "<div style='margin:16px 0 6px;font-weight:bold;color:" + AZUL + "'>" + escapar(titulo) + "</div>";
    }

    /** Versión en texto para la consola (--probar y --consola). */
    public static String texto(Respuesta r) {
        String salto = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append(r.esAviso() ? "Aviso: " : r.esExito() ? "Éxito: " : "Error: ").append(r.mensaje());
        for (Bloque bloque : r.bloques()) {
            sb.append(salto).append(salto).append(bloque.texto());
        }
        if (!r.adjuntos().isEmpty()) {
            sb.append(salto).append(salto).append("Archivo adjunto: ").append(nombresAdjuntos(r));
        }
        return sb.toString();
    }

    private static String nombresAdjuntos(Respuesta r) {
        StringBuilder sb = new StringBuilder();
        for (ArchivoAdjunto a : r.adjuntos()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(a.nombre()).append(" (").append(Math.max(1, a.contenido().length / 1024)).append(" KB)");
        }
        return sb.toString();
    }

    public static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String escaparConSaltos(String texto) {
        return escapar(texto).replace("\r\n", "\n").replace("\n", "<br>");
    }
}
