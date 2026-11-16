package bo.tecnoweb.grupo17sa.datos;

/** Filtro de los comandos LISxxx: "*" lista todo; otro texto busca coincidencias con ILIKE. */
final class Filtro {

    private Filtro() {
    }

    static boolean todos(String filtro) {
        return filtro == null || filtro.isBlank() || filtro.trim().equals("*");
    }

    /** Patrón para ILIKE: escapa % y _ para que se busquen como texto. */
    static String patron(String filtro) {
        String limpio = filtro.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + limpio + "%";
    }
}
