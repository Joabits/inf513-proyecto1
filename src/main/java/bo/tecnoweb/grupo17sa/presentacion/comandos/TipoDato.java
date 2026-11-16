package bo.tecnoweb.grupo17sa.presentacion.comandos;

/** Tipos de dato de los parámetros; se validan antes de pasar los valores a PostgreSQL. */
public enum TipoDato {
    TEXTO("texto"),
    ENTERO("número entero"),
    DECIMAL("número decimal"),
    FECHA("fecha AAAA-MM-DD"),
    BOOLEANO("SI o NO"),
    CORREO("correo electrónico"),
    URL("enlace http(s)"),
    OPCION("una de las opciones"),
    FILTRO("\"*\" para todo o un texto a buscar");

    private final String descripcion;

    TipoDato(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
