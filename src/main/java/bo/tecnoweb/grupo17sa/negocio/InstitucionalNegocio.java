package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.SeccionInstitucionalDAO;
import bo.tecnoweb.grupo17sa.modelo.SeccionInstitucional;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** RF-02 Gestión de información institucional: historia, misión, visión, objetivos, perfil, campo y estructura. */
public class InstitucionalNegocio {

    private final SeccionInstitucionalDAO secciones = new SeccionInstitucionalDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    public List<SeccionInstitucional> listar(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> secciones.listar(con, filtro));
    }

    public SeccionInstitucional ver(String clave) throws SQLException, ErrorNegocio {
        String limpia = normalizarClave(clave);
        return Transaccion.ejecutar(con -> {
            SeccionInstitucional seccion = secciones.buscar(con, limpia);
            if (seccion == null) {
                throw new ErrorNegocio("No existe la sección «" + clave + "». Secciones disponibles: " + claves(con) + ".");
            }
            return seccion;
        });
    }

    public void modificar(String usuario, String clave, String titulo, String contenido) throws SQLException, ErrorNegocio {
        String limpia = normalizarClave(clave);
        Transaccion.ejecutar(con -> {
            if (!secciones.modificar(con, limpia, titulo, contenido)) {
                throw new ErrorNegocio("No existe la sección «" + clave + "». Secciones disponibles: " + claves(con) + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, "CU2", "seccion_institucional", limpia, titulo);
            return null;
        });
    }

    /** "Misión" → "mision", "Perfil profesional" → "perfil_profesional". */
    static String normalizarClave(String clave) {
        String sinTildes = Normalizer.normalize(clave.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    private String claves(Connection con) throws SQLException {
        List<String> claves = new ArrayList<>();
        for (SeccionInstitucional s : secciones.listar(con, "*")) {
            claves.add(s.clave());
        }
        return String.join(", ", claves);
    }
}
