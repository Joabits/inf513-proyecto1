package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.ArticuloRevistaDAO;
import bo.tecnoweb.grupo17sa.datos.EdicionRevistaDAO;
import bo.tecnoweb.grupo17sa.modelo.ArticuloRevista;
import bo.tecnoweb.grupo17sa.modelo.EdicionRevista;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Year;
import java.util.List;

/**
 * CU4 Revista INNOVAR (RF-08): ediciones y sus artículos. Una edición nueva queda en borrador; publicarla es una
 * decisión aparte (PUBREV). El visitante solo ve las ediciones publicadas.
 */
public class RevistaNegocio {

    private static final String MODULO = InvestigacionNegocio.MODULO;
    public static final int PRIMER_ANIO = 1990;

    /** Lo que muestra VERREV. */
    public record EdicionCompleta(EdicionRevista edicion, List<ArticuloRevista> articulos) {
    }

    private final EdicionRevistaDAO ediciones = new EdicionRevistaDAO();
    private final ArticuloRevistaDAO articulos = new ArticuloRevistaDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    // ------------------------------------------------------------ Ediciones

    public List<EdicionRevista> listar(String filtro, boolean verBorradores) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> ediciones.listar(con, filtro, !verBorradores));
    }

    public EdicionCompleta ver(int id, boolean verBorradores) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            EdicionRevista e = ediciones.buscar(con, id);
            if (e == null || (!e.publicada() && !verBorradores)) {
                throw new ErrorNegocio("No existe una edición publicada con ID " + id + ". Envíe LISREV[\"*\"] para verlas.");
            }
            return new EdicionCompleta(e, articulos.listarDeEdicion(con, id));
        });
    }

    public int insertarEdicion(String usuario, EdicionRevista e) throws SQLException, ErrorNegocio {
        validar(e);
        return Transaccion.ejecutar(con -> {
            exigirNumeroLibre(con, e, 0);
            int id = ediciones.insertar(con, e);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "revista_edicion", id, e.nombre());
            return id;
        });
    }

    public void modificarEdicion(String usuario, EdicionRevista e) throws SQLException, ErrorNegocio {
        validar(e);
        Transaccion.ejecutar(con -> {
            exigirNumeroLibre(con, e, e.id());
            if (!ediciones.modificar(con, e)) {
                throw new ErrorNegocio("No existe una edición con ID " + e.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "revista_edicion", e.id(), e.nombre());
            return null;
        });
    }

    public EdicionRevista publicarEdicion(String usuario, int id, boolean publicada) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            EdicionRevista e = edicionExistente(con, id);
            ediciones.publicar(con, id, publicada);
            bitacora.registrar(con, usuario, BitacoraNegocio.PUBLICAR, MODULO, "revista_edicion", id,
                    (publicada ? "Publicada: " : "Oculta: ") + e.nombre());
            return e;
        });
    }

    /** Borra la edición y sus artículos. */
    public EdicionRevista eliminarEdicion(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            EdicionRevista e = edicionExistente(con, id);
            ediciones.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "revista_edicion", id,
                    e.nombre() + " (con " + e.articulos() + " artículo(s))");
            return e;
        });
    }

    // ------------------------------------------------------------ Artículos

    public int insertarArticulo(String usuario, ArticuloRevista a) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            edicionExistente(con, a.edicionId());
            int id = articulos.insertar(con, a);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "revista_articulo", id, a.titulo());
            return id;
        });
    }

    public void modificarArticulo(String usuario, ArticuloRevista a) throws SQLException, ErrorNegocio {
        Transaccion.ejecutar(con -> {
            if (!articulos.modificar(con, a)) {
                throw new ErrorNegocio("No existe un artículo con ID " + a.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "revista_articulo", a.id(), a.titulo());
            return null;
        });
    }

    public ArticuloRevista eliminarArticulo(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            ArticuloRevista a = articulos.buscar(con, id);
            if (a == null) {
                throw new ErrorNegocio("No existe un artículo con ID " + id + ".");
            }
            articulos.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "revista_articulo", id, a.titulo());
            return a;
        });
    }

    // ------------------------------------------------------------ Reglas

    /** "" sin volumen; si no, un número de 1 a 99. */
    public static Integer leerVolumen(String valor) throws ErrorNegocio {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            int volumen = Integer.parseInt(valor.trim());
            if (volumen < 1 || volumen > 99) {
                throw new NumberFormatException();
            }
            return volumen;
        } catch (NumberFormatException e) {
            throw new ErrorNegocio("El volumen debe ser un número de 1 a 99, o vacío (\"\"); se recibió «" + valor + "».");
        }
    }

    private static void validar(EdicionRevista e) throws ErrorNegocio {
        int maximo = Year.now().getValue() + 1;
        if (e.anio() < PRIMER_ANIO || e.anio() > maximo) {
            throw new ErrorNegocio("El año debe estar entre " + PRIMER_ANIO + " y " + maximo + ".");
        }
        if (e.numero() < 1 || e.numero() > 999) {
            throw new ErrorNegocio("El número de la edición debe estar entre 1 y 999.");
        }
    }

    private void exigirNumeroLibre(Connection con, EdicionRevista e, int idActual) throws SQLException, ErrorNegocio {
        EdicionRevista otra = ediciones.buscarPorNumero(con, e.anio(), e.numero());
        if (otra != null && otra.id() != idActual) {
            throw new ErrorNegocio("Ya existe la edición " + otra.nombre() + " (ID " + otra.id() + ").");
        }
    }

    private EdicionRevista edicionExistente(Connection con, int id) throws SQLException, ErrorNegocio {
        EdicionRevista e = ediciones.buscar(con, id);
        if (e == null) {
            throw new ErrorNegocio("No existe una edición con ID " + id + ".");
        }
        return e;
    }
}
