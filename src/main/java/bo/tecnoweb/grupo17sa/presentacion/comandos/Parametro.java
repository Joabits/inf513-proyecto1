package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.presentacion.lexico.Comando;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Un parámetro que espera un comando: nombre, tipo, si es obligatorio y el largo máximo
 * (el mismo de la columna en la base de datos). Los opcionales se envían vacíos: "".
 */
public final class Parametro {

    private final String nombre;
    private final TipoDato tipo;
    private final boolean obligatorio;
    private final int maximo;
    private final List<String> opciones;

    private Parametro(String nombre, TipoDato tipo, boolean obligatorio, int maximo, List<String> opciones) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.obligatorio = obligatorio;
        this.maximo = maximo;
        this.opciones = opciones;
    }

    private Parametro(String nombre, TipoDato tipo, boolean obligatorio, int maximo) {
        this(nombre, tipo, obligatorio, maximo, List.of());
    }

    public static Parametro texto(String nombre, int maximo) {
        return new Parametro(nombre, TipoDato.TEXTO, true, maximo);
    }

    public static Parametro textoOpcional(String nombre, int maximo) {
        return new Parametro(nombre, TipoDato.TEXTO, false, maximo);
    }

    public static Parametro entero(String nombre) {
        return new Parametro(nombre, TipoDato.ENTERO, true, 0);
    }

    public static Parametro enteroOpcional(String nombre) {
        return new Parametro(nombre, TipoDato.ENTERO, false, 0);
    }

    public static Parametro decimal(String nombre) {
        return new Parametro(nombre, TipoDato.DECIMAL, true, 0);
    }

    public static Parametro fecha(String nombre) {
        return new Parametro(nombre, TipoDato.FECHA, true, 0);
    }

    public static Parametro fechaOpcional(String nombre) {
        return new Parametro(nombre, TipoDato.FECHA, false, 0);
    }

    public static Parametro booleano(String nombre) {
        return new Parametro(nombre, TipoDato.BOOLEANO, true, 0);
    }

    public static Parametro correo(String nombre, int maximo) {
        return new Parametro(nombre, TipoDato.CORREO, true, maximo);
    }

    public static Parametro correoOpcional(String nombre, int maximo) {
        return new Parametro(nombre, TipoDato.CORREO, false, maximo);
    }

    public static Parametro url(String nombre, int maximo) {
        return new Parametro(nombre, TipoDato.URL, true, maximo);
    }

    public static Parametro urlOpcional(String nombre, int maximo) {
        return new Parametro(nombre, TipoDato.URL, false, maximo);
    }

    /** Uno de los valores permitidos (los mismos del CHECK de la tabla). Se aceptan minúsculas y tildes. */
    public static Parametro opcion(String nombre, String... valores) {
        return new Parametro(nombre, TipoDato.OPCION, true, 0, List.of(valores));
    }

    /** "*" para listar todo o un texto a buscar. */
    public static Parametro filtro() {
        return new Parametro("filtro", TipoDato.FILTRO, true, 100);
    }

    public String nombre() {
        return nombre;
    }

    /** Por ejemplo "orden (número entero)" o "estado (VIGENTE, EN_TRAMITE o CONCLUIDO)". */
    public String describir() {
        String texto = tipo == TipoDato.OPCION ? String.join(", ", opciones) : tipo.descripcion();
        if (maximo > 0 && tipo != TipoDato.FILTRO) {
            texto += ", máx. " + maximo;
        }
        if (!obligatorio) {
            texto += ", opcional";
        }
        return nombre + " (" + texto + ")";
    }

    /** Devuelve null si el valor es válido, o el motivo por el que no lo es. */
    public String validar(String valor) {
        if (valor.isEmpty()) {
            return obligatorio ? "no puede estar vacío" : null;
        }
        if (maximo > 0 && valor.length() > maximo) {
            return "admite como máximo " + maximo + " caracteres y tiene " + valor.length();
        }
        switch (tipo) {
            case ENTERO:
                return valor.matches("[+-]?\\d{1,9}") ? null : "debe ser un número entero";
            case DECIMAL:
                return valor.matches("[+-]?\\d{1,12}([.,]\\d{1,4})?") ? null : "debe ser un número decimal, por ejemplo 150.50";
            case FECHA:
                return (Comando.leerFecha(valor) != null || Comando.leerFechaHora(valor) != null) ? null : "debe ser una fecha válida AAAA-MM-DD o DD/MM/AAAA";
            case BOOLEANO:
                return valor.toUpperCase(Locale.ROOT).matches("SI|SÍ|NO|TRUE|FALSE") ? null : "debe ser SI o NO";
            case CORREO:
                return valor.matches("[^@\\s\"]+@[^@\\s\"]+\\.[^@\\s\"]+") ? null : "debe ser un correo electrónico válido";
            case URL:
                return valor.matches("https?://\\S+") ? null : "debe ser un enlace que empiece con http:// o https://";
            case OPCION:
                return opciones.contains(normalizarOpcion(valor)) ? null : "debe ser uno de estos valores: " + String.join(", ", opciones);
            default:
                return null;
        }
    }

    /** "práctica" → "PRACTICA", "en trámite" → "EN_TRAMITE", "sin empleo" → "SIN_EMPLEO". */
    public static String normalizarOpcion(String valor) {
        String sinTildes = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }
}
