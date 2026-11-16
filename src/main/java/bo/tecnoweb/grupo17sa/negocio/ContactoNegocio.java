package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.ConfiguracionDAO;
import bo.tecnoweb.grupo17sa.datos.RedSocialDAO;
import bo.tecnoweb.grupo17sa.modelo.DatoConfiguracion;
import bo.tecnoweb.grupo17sa.modelo.RedSocial;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/** RF-16 Configuración de contacto y redes sociales, y los datos configurables del portal (LISCFG/MODCFG). */
public class ContactoNegocio {

    /** Lo que muestra el comando CONTACTO. */
    public record DatosContacto(List<DatoConfiguracion> datos, List<RedSocial> redes) {
    }

    private static final String MODULO = "CU2";

    private final ConfiguracionDAO configuracion = new ConfiguracionDAO();
    private final RedSocialDAO redes = new RedSocialDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    public DatosContacto contacto() throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> new DatosContacto(configuracion.listarGrupo(con, "contacto"), redes.listar(con, "*")));
    }

    // ------------------------------------------------------------ Datos configurables

    public List<DatoConfiguracion> listarConfiguracion(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> configuracion.listar(con, filtro));
    }

    public void modificarConfiguracion(String usuario, String clave, String valor) throws SQLException, ErrorNegocio {
        String limpia = clave.trim().toLowerCase(Locale.ROOT);
        String nuevo = valor == null ? "" : valor;
        if (limpia.endsWith("_url") && !nuevo.isEmpty() && !nuevo.matches("https?://\\S+")) {
            throw new ErrorNegocio("El valor de " + limpia + " debe ser un enlace que empiece con http:// o https://.");
        }
        Transaccion.ejecutar(con -> {
            if (!configuracion.modificar(con, limpia, nuevo)) {
                throw new ErrorNegocio("No existe el dato «" + clave + "». Envíe LISCFG[\"*\"] para ver las claves.");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "configuracion", limpia, nuevo);
            return null;
        });
    }

    // ------------------------------------------------------------ Redes sociales

    public List<RedSocial> listarRedes(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> redes.listar(con, filtro));
    }

    public int insertarRed(String usuario, RedSocial red) throws SQLException, ErrorNegocio {
        InicioNegocio.validarOrden(red.orden());
        return Transaccion.ejecutar(con -> {
            int id = redes.insertar(con, red);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "red_social", id, red.nombre());
            return id;
        });
    }

    public void modificarRed(String usuario, RedSocial red) throws SQLException, ErrorNegocio {
        InicioNegocio.validarOrden(red.orden());
        Transaccion.ejecutar(con -> {
            if (!redes.modificar(con, red)) {
                throw new ErrorNegocio("No existe una red social con ID " + red.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "red_social", red.id(), red.nombre());
            return null;
        });
    }

    public RedSocial eliminarRed(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            RedSocial red = redes.buscar(con, id);
            if (red == null) {
                throw new ErrorNegocio("No existe una red social con ID " + id + ".");
            }
            redes.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "red_social", id, red.nombre());
            return red;
        });
    }
}
