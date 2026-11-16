package bo.tecnoweb.grupo17sa.presentacion.vista;

import java.util.Objects;

/** Un título con un texto (por ejemplo, el contenido de una sección institucional). */
public class Parrafo implements Bloque {

    private final String titulo;
    private final String contenido;

    public Parrafo(String titulo, String contenido) {
        this.titulo = Objects.toString(titulo, "");
        this.contenido = Objects.toString(contenido, "");
    }

    @Override
    public String html() {
        return PlantillaHTML.tituloSeccion(titulo)
                + "<div>" + PlantillaHTML.escaparConSaltos(contenido) + "</div>";
    }

    @Override
    public String texto() {
        return titulo + System.lineSeparator() + contenido;
    }
}
