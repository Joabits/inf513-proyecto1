package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.BusquedaDAO;
import bo.tecnoweb.grupo17sa.modelo.ResultadoBusqueda;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** CU7 Búsqueda interna global (RF-17): en el contenido publicado de todos los casos de uso. */
public class BusquedaNegocio {

    public static final int MINIMO = 3;
    public static final int MAXIMO_PALABRAS = 5;

    private final BusquedaDAO busqueda = new BusquedaDAO();

    public List<ResultadoBusqueda> buscar(String texto) throws SQLException, ErrorNegocio {
        String limpio = texto.trim().replaceAll("\\s+", " ");
        if (limpio.length() < MINIMO) {
            throw new ErrorNegocio("Escriba al menos " + MINIMO + " caracteres para buscar.");
        }
        List<String> palabras = palabras(limpio);
        if (palabras.isEmpty()) {
            throw new ErrorNegocio("Escriba al menos una palabra de dos letras o más.");
        }
        return Transaccion.ejecutar(con -> busqueda.buscar(con, palabras));
    }

    /** «  Reglamento de TITULACIÓN » → [reglamento, de, titulacion]: sin tildes (la ñ se conserva), sin repetir, máximo 5. */
    public static List<String> palabras(String texto) {
        List<String> palabras = new ArrayList<>();
        for (String palabra : texto.toLowerCase(Locale.ROOT).split("\\s+")) {
            String normal = quitarTildes(palabra);
            if (normal.length() >= 2 && !palabras.contains(normal)) {
                palabras.add(normal);
            }
            if (palabras.size() == MAXIMO_PALABRAS) {
                break;
            }
        }
        return palabras;
    }

    private static String quitarTildes(String texto) {
        StringBuilder sb = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            int i = BusquedaDAO.CON_TILDE.indexOf(c);
            sb.append(i >= 0 ? BusquedaDAO.SIN_TILDE.charAt(i) : c);
        }
        return sb.toString();
    }
}
