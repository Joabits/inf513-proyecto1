package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.AccesoRapidoDAO;
import bo.tecnoweb.grupo17sa.datos.BannerDAO;
import bo.tecnoweb.grupo17sa.datos.ConfiguracionDAO;
import bo.tecnoweb.grupo17sa.datos.DestacadoDAO;
import bo.tecnoweb.grupo17sa.modelo.AccesoRapido;
import bo.tecnoweb.grupo17sa.modelo.Banner;
import bo.tecnoweb.grupo17sa.modelo.Destacado;

import java.sql.SQLException;
import java.util.List;

/** RF-01 Gestión de la página de inicio: banners, contenidos destacados y accesos rápidos. */
public class InicioNegocio {

    /** Lo que muestra el comando INICIO. */
    public record PaginaInicio(String titulo, String bienvenida, List<Banner> banners,
                               List<Destacado> destacados, List<AccesoRapido> accesos) {
    }

    private static final String MODULO = "CU2";

    private final ConfiguracionDAO configuracion = new ConfiguracionDAO();
    private final BannerDAO banners = new BannerDAO();
    private final DestacadoDAO destacados = new DestacadoDAO();
    private final AccesoRapidoDAO accesos = new AccesoRapidoDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    public PaginaInicio inicio() throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> new PaginaInicio(
                configuracion.valor(con, "titulo_portal"),
                configuracion.valor(con, "mensaje_bienvenida"),
                banners.listar(con, "*"),
                destacados.listar(con, "*"),
                accesos.listar(con, "*")));
    }

    // ------------------------------------------------------------ Banners

    public List<Banner> listarBanners(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> banners.listar(con, filtro));
    }

    public int insertarBanner(String usuario, Banner banner) throws SQLException, ErrorNegocio {
        validarOrden(banner.orden());
        return Transaccion.ejecutar(con -> {
            int id = banners.insertar(con, banner);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "banner", id, banner.titulo());
            return id;
        });
    }

    public void modificarBanner(String usuario, Banner banner) throws SQLException, ErrorNegocio {
        validarOrden(banner.orden());
        Transaccion.ejecutar(con -> {
            if (!banners.modificar(con, banner)) {
                throw new ErrorNegocio("No existe un banner con ID " + banner.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "banner", banner.id(), banner.titulo());
            return null;
        });
    }

    public Banner eliminarBanner(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Banner banner = banners.buscar(con, id);
            if (banner == null) {
                throw new ErrorNegocio("No existe un banner con ID " + id + ".");
            }
            banners.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "banner", id, banner.titulo());
            return banner;
        });
    }

    // ------------------------------------------------------------ Contenidos destacados

    public List<Destacado> listarDestacados(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> destacados.listar(con, filtro));
    }

    public int insertarDestacado(String usuario, Destacado destacado) throws SQLException, ErrorNegocio {
        validarOrden(destacado.orden());
        return Transaccion.ejecutar(con -> {
            int id = destacados.insertar(con, destacado);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "destacado", id, destacado.titulo());
            return id;
        });
    }

    public void modificarDestacado(String usuario, Destacado destacado) throws SQLException, ErrorNegocio {
        validarOrden(destacado.orden());
        Transaccion.ejecutar(con -> {
            if (!destacados.modificar(con, destacado)) {
                throw new ErrorNegocio("No existe un contenido destacado con ID " + destacado.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "destacado", destacado.id(), destacado.titulo());
            return null;
        });
    }

    public Destacado eliminarDestacado(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Destacado destacado = destacados.buscar(con, id);
            if (destacado == null) {
                throw new ErrorNegocio("No existe un contenido destacado con ID " + id + ".");
            }
            destacados.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "destacado", id, destacado.titulo());
            return destacado;
        });
    }

    // ------------------------------------------------------------ Accesos rápidos

    public List<AccesoRapido> listarAccesos(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> accesos.listar(con, filtro));
    }

    public int insertarAcceso(String usuario, AccesoRapido acceso) throws SQLException, ErrorNegocio {
        validarOrden(acceso.orden());
        return Transaccion.ejecutar(con -> {
            int id = accesos.insertar(con, acceso);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "acceso_rapido", id, acceso.nombre());
            return id;
        });
    }

    public void modificarAcceso(String usuario, AccesoRapido acceso) throws SQLException, ErrorNegocio {
        validarOrden(acceso.orden());
        Transaccion.ejecutar(con -> {
            if (!accesos.modificar(con, acceso)) {
                throw new ErrorNegocio("No existe un acceso rápido con ID " + acceso.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "acceso_rapido", acceso.id(), acceso.nombre());
            return null;
        });
    }

    public AccesoRapido eliminarAcceso(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            AccesoRapido acceso = accesos.buscar(con, id);
            if (acceso == null) {
                throw new ErrorNegocio("No existe un acceso rápido con ID " + id + ".");
            }
            accesos.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "acceso_rapido", id, acceso.nombre());
            return acceso;
        });
    }

    static void validarOrden(int orden) throws ErrorNegocio {
        if (orden < 1) {
            throw new ErrorNegocio("El orden debe ser 1 o mayor.");
        }
    }
}
