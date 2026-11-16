package bo.tecnoweb.grupo17sa.presentacion.vista;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Tabla de registros para la respuesta. Los valores que son enlaces http(s) se muestran como enlace. */
public class TablaHTML implements Bloque {

    private final String titulo;
    private final String[] columnas;
    private final List<String[]> filas = new ArrayList<>();
    private boolean mostrarCantidad = true;

    public TablaHTML(String titulo, String... columnas) {
        this.titulo = titulo;
        this.columnas = columnas;
    }

    public TablaHTML fila(Object... celdas) {
        String[] fila = new String[columnas.length];
        for (int i = 0; i < columnas.length; i++) {
            fila[i] = i < celdas.length ? Objects.toString(celdas[i], "") : "";
        }
        filas.add(fila);
        return this;
    }

    /** Para tablas de datos (por ejemplo, contacto) donde no tiene sentido mostrar la cantidad de filas. */
    public TablaHTML sinCantidad() {
        mostrarCantidad = false;
        return this;
    }

    public int cantidad() {
        return filas.size();
    }

    @Override
    public String html() {
        StringBuilder sb = new StringBuilder();
        sb.append(PlantillaHTML.tituloSeccion(mostrarCantidad ? titulo + " (" + filas.size() + ")" : titulo));
        if (filas.isEmpty()) {
            return sb.append("<p style='color:#777;margin:0'>Sin registros.</p>").toString();
        }
        sb.append("<table cellpadding='5' cellspacing='0' style='border-collapse:collapse;width:100%;font-size:13px'>");
        sb.append("<tr>");
        for (String columna : columnas) {
            sb.append("<th style='background:#f2f2f2;color:#222;text-align:left;border:1px solid #ccc'>")
                    .append(PlantillaHTML.escapar(columna)).append("</th>");
        }
        sb.append("</tr>");
        for (int f = 0; f < filas.size(); f++) {
            sb.append("<tr>");
            for (String celda : filas.get(f)) {
                sb.append("<td style='border:1px solid #ccc;vertical-align:top'>")
                        .append(celdaHtml(celda)).append("</td>");
            }
            sb.append("</tr>");
        }
        return sb.append("</table>").toString();
    }

    @Override
    public String texto() {
        String salto = System.lineSeparator();
        StringBuilder sb = new StringBuilder(titulo);
        if (mostrarCantidad) {
            sb.append(" (").append(filas.size()).append(")");
        }
        sb.append(salto);
        if (filas.isEmpty()) {
            return sb.append("  Sin registros.").toString();
        }
        sb.append("  ").append(String.join(" | ", columnas)).append(salto);
        for (String[] fila : filas) {
            sb.append("  ").append(String.join(" | ", fila).replace('\n', ' ')).append(salto);
        }
        return sb.toString().stripTrailing();
    }

    private static String celdaHtml(String valor) {
        if (valor.matches("https?://\\S+")) {
            String enlace = PlantillaHTML.escapar(valor);
            return "<a href='" + enlace + "' style='color:#21366E;word-break:break-all'>" + enlace + "</a>";
        }
        return PlantillaHTML.escaparConSaltos(valor);
    }
}
