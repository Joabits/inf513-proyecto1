package bo.tecnoweb.grupo17sa.presentacion.comandos;

import java.util.ArrayList;
import java.util.List;

/** Lo que el sistema sabe de un comando: nombre, descripción, si es público y sus parámetros en orden. */
public final class DefinicionComando {

    private final String nombre;
    private final String descripcion;
    private final boolean publico;
    private final List<Parametro> parametros;

    private DefinicionComando(String nombre, String descripcion, boolean publico, Parametro... parametros) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.publico = publico;
        this.parametros = List.of(parametros);
    }

    /** Comando que puede usar cualquier persona. */
    public static DefinicionComando publico(String nombre, String descripcion, Parametro... parametros) {
        return new DefinicionComando(nombre, descripcion, true, parametros);
    }

    /** Comando que necesita permiso sobre el módulo (caso de uso). */
    public static DefinicionComando privado(String nombre, String descripcion, Parametro... parametros) {
        return new DefinicionComando(nombre, descripcion, false, parametros);
    }

    public String nombre() {
        return nombre;
    }

    public String descripcion() {
        return descripcion;
    }

    public boolean esPublico() {
        return publico;
    }

    public List<Parametro> parametros() {
        return parametros;
    }

    /** Por ejemplo INSBAN["titulo","subtitulo","imagen_url","enlace","orden"]. */
    public String sintaxis() {
        if (parametros.isEmpty()) {
            return nombre;
        }
        List<String> nombres = new ArrayList<>();
        for (Parametro p : parametros) {
            nombres.add("\"" + p.nombre() + "\"");
        }
        return nombre + "[" + String.join(",", nombres) + "]";
    }

    /** Descripción con el tipo de cada parámetro (para HELP). */
    public String descripcionCompleta() {
        if (parametros.isEmpty()) {
            return descripcion;
        }
        List<String> detalle = new ArrayList<>();
        for (Parametro p : parametros) {
            detalle.add(p.describir());
        }
        return descripcion + "\nParámetros: " + String.join("; ", detalle);
    }
}
