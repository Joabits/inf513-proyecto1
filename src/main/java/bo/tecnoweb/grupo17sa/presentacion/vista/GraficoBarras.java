package bo.tecnoweb.grupo17sa.presentacion.vista;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gráfico de barras horizontales hecho solo con HTML (sin imágenes), para que Gmail lo muestre
 * sin pedir permiso para cargar imágenes. Cada fila: etiqueta, barra, cantidad y porcentaje.
 */
public class GraficoBarras implements Bloque {

    private final String titulo;
    private final List<String> etiquetas = new ArrayList<>();
    private final List<Long> valores = new ArrayList<>();

    public GraficoBarras(String titulo) {
        this.titulo = titulo;
    }

    public GraficoBarras agregar(String etiqueta, long valor) {
        etiquetas.add(etiqueta);
        valores.add(valor);
        return this;
    }

    private long total() {
        long total = 0;
        for (long v : valores) {
            total += v;
        }
        return total;
    }

    private long maximo() {
        long maximo = 0;
        for (long v : valores) {
            maximo = Math.max(maximo, v);
        }
        return maximo;
    }

    @Override
    public String html() {
        StringBuilder sb = new StringBuilder(PlantillaHTML.tituloSeccion(titulo));
        if (valores.isEmpty()) {
            return sb.append("<p style='margin:0;color:#777'>Sin datos.</p>").toString();
        }
        long total = total();
        long maximo = Math.max(1, maximo());
        sb.append("<table cellpadding='4' cellspacing='0' style='border-collapse:collapse;width:100%;font-size:13px'>");
        for (int i = 0; i < valores.size(); i++) {
            long valor = valores.get(i);
            int ancho = valor == 0 ? 0 : (int) Math.max(1, Math.round(valor * 100.0 / maximo));
            sb.append("<tr>")
                    .append("<td style='width:32%;padding-right:8px'>").append(PlantillaHTML.escapar(etiquetas.get(i))).append("</td>")
                    .append("<td><div style='background:#21366E;height:12px;width:").append(ancho).append("%'></div></td>")
                    .append("<td style='width:90px;text-align:right;white-space:nowrap'>").append(valor)
                    .append(" (").append(porcentaje(valor, total)).append(")</td>")
                    .append("</tr>");
        }
        return sb.append("</table>").toString();
    }

    @Override
    public String texto() {
        String salto = System.lineSeparator();
        StringBuilder sb = new StringBuilder(titulo);
        if (valores.isEmpty()) {
            return sb.append(salto).append("  Sin datos.").toString();
        }
        long total = total();
        for (int i = 0; i < valores.size(); i++) {
            sb.append(salto).append("  ").append(etiquetas.get(i)).append(": ").append(valores.get(i))
                    .append(" (").append(porcentaje(valores.get(i), total)).append(")");
        }
        return sb.toString();
    }

    static String porcentaje(long valor, long total) {
        return total == 0 ? "0%" : String.format(Locale.ROOT, "%.0f%%", valor * 100.0 / total);
    }
}
