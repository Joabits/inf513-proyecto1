package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.ConfiguracionDAO;
import bo.tecnoweb.grupo17sa.datos.MiembroInnovaDAO;
import bo.tecnoweb.grupo17sa.datos.ProyectoInnovaDAO;
import bo.tecnoweb.grupo17sa.datos.ServicioInnovaDAO;
import bo.tecnoweb.grupo17sa.datos.SolicitudInnovaDAO;
import bo.tecnoweb.grupo17sa.modelo.MiembroInnova;
import bo.tecnoweb.grupo17sa.modelo.ProyectoInnova;
import bo.tecnoweb.grupo17sa.modelo.ServicioInnova;
import bo.tecnoweb.grupo17sa.modelo.SolicitudInnova;

import java.sql.SQLException;
import java.time.Year;
import java.util.List;

/**
 * RF-10 Módulo Empresa Junior INNOVA: descripción, servicios, equipo y proyectos realizados,
 * y las solicitudes de empresas o postulaciones de estudiantes que llegan por correo.
 */
public class InnovaNegocio {

    /** Lo que muestra el comando INNOVA. */
    public record PaginaInnova(String descripcion, List<ServicioInnova> servicios, List<MiembroInnova> equipo,
                               List<ProyectoInnova> proyectos) {
    }

    /** Máximo de solicitudes pendientes que puede tener un mismo correo (evita que se llene de correos repetidos). */
    public static final int MAXIMO_PENDIENTES = 3;

    private static final String MODULO = "CU5";
    private static final String CLAVE_DESCRIPCION = "innova_descripcion";

    private final ConfiguracionDAO configuracion = new ConfiguracionDAO();
    private final ServicioInnovaDAO servicios = new ServicioInnovaDAO();
    private final MiembroInnovaDAO miembros = new MiembroInnovaDAO();
    private final ProyectoInnovaDAO proyectos = new ProyectoInnovaDAO();
    private final SolicitudInnovaDAO solicitudes = new SolicitudInnovaDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    public PaginaInnova pagina() throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> new PaginaInnova(configuracion.valor(con, CLAVE_DESCRIPCION),
                servicios.listar(con, "*"), miembros.listar(con, "*"), proyectos.listar(con, "*")));
    }

    public void modificarDescripcion(String usuario, String descripcion) throws SQLException, ErrorNegocio {
        Transaccion.ejecutar(con -> {
            if (!configuracion.modificar(con, CLAVE_DESCRIPCION, descripcion)) {
                throw new ErrorNegocio("Falta el dato innova_descripcion; ejecute el programa con --instalar-bd.");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "configuracion", CLAVE_DESCRIPCION, descripcion);
            return null;
        });
    }

    // ------------------------------------------------------------ Servicios

    public List<ServicioInnova> listarServicios(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> servicios.listar(con, filtro));
    }

    public int insertarServicio(String usuario, ServicioInnova servicio) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            int id = servicios.insertar(con, servicio);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "innova_servicio", id, servicio.nombre());
            return id;
        });
    }

    public void modificarServicio(String usuario, ServicioInnova servicio) throws SQLException, ErrorNegocio {
        Transaccion.ejecutar(con -> {
            if (!servicios.modificar(con, servicio)) {
                throw new ErrorNegocio("No existe un servicio de INNOVA con ID " + servicio.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "innova_servicio", servicio.id(), servicio.nombre());
            return null;
        });
    }

    public ServicioInnova eliminarServicio(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            ServicioInnova servicio = servicios.buscar(con, id);
            if (servicio == null) {
                throw new ErrorNegocio("No existe un servicio de INNOVA con ID " + id + ".");
            }
            servicios.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "innova_servicio", id, servicio.nombre());
            return servicio;
        });
    }

    // ------------------------------------------------------------ Equipo

    public List<MiembroInnova> listarMiembros(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> miembros.listar(con, filtro));
    }

    public int insertarMiembro(String usuario, MiembroInnova miembro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            int id = miembros.insertar(con, miembro);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "innova_miembro", id, miembro.nombre());
            return id;
        });
    }

    public void modificarMiembro(String usuario, MiembroInnova miembro) throws SQLException, ErrorNegocio {
        Transaccion.ejecutar(con -> {
            if (!miembros.modificar(con, miembro)) {
                throw new ErrorNegocio("No existe un miembro de INNOVA con ID " + miembro.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "innova_miembro", miembro.id(), miembro.nombre());
            return null;
        });
    }

    public MiembroInnova eliminarMiembro(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            MiembroInnova miembro = miembros.buscar(con, id);
            if (miembro == null) {
                throw new ErrorNegocio("No existe un miembro de INNOVA con ID " + id + ".");
            }
            miembros.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "innova_miembro", id, miembro.nombre());
            return miembro;
        });
    }

    // ------------------------------------------------------------ Proyectos realizados

    public List<ProyectoInnova> listarProyectos(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> proyectos.listar(con, filtro));
    }

    public int insertarProyecto(String usuario, ProyectoInnova proyecto) throws SQLException, ErrorNegocio {
        validarAnio(proyecto.anio());
        return Transaccion.ejecutar(con -> {
            int id = proyectos.insertar(con, proyecto);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "innova_proyecto", id, proyecto.nombre());
            return id;
        });
    }

    public void modificarProyecto(String usuario, ProyectoInnova proyecto) throws SQLException, ErrorNegocio {
        validarAnio(proyecto.anio());
        Transaccion.ejecutar(con -> {
            if (!proyectos.modificar(con, proyecto)) {
                throw new ErrorNegocio("No existe un proyecto de INNOVA con ID " + proyecto.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "innova_proyecto", proyecto.id(), proyecto.nombre());
            return null;
        });
    }

    public ProyectoInnova eliminarProyecto(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            ProyectoInnova proyecto = proyectos.buscar(con, id);
            if (proyecto == null) {
                throw new ErrorNegocio("No existe un proyecto de INNOVA con ID " + id + ".");
            }
            proyectos.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "innova_proyecto", id, proyecto.nombre());
            return proyecto;
        });
    }

    private static void validarAnio(int anio) throws ErrorNegocio {
        int actual = Year.now().getValue();
        if (anio < 2000 || anio > actual) {
            throw new ErrorNegocio("El año del proyecto debe estar entre 2000 y " + actual + ".");
        }
    }

    // ------------------------------------------------------------ Solicitudes y postulaciones

    /** Una empresa pide un servicio o un estudiante postula; el correo es el del remitente. */
    public int solicitar(SolicitudInnova solicitud) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            if (solicitudes.contarPendientes(con, solicitud.correo()) >= MAXIMO_PENDIENTES) {
                throw new ErrorNegocio("Ya tiene " + MAXIMO_PENDIENTES + " solicitudes pendientes. Espere a que se atiendan;"
                        + " puede ver su estado con MISSOL.");
            }
            int id = solicitudes.insertar(con, solicitud);
            bitacora.registrar(con, solicitud.correo(), BitacoraNegocio.CREAR, MODULO, "innova_solicitud", id,
                    solicitud.tipo() + ": " + solicitud.nombre());
            return id;
        });
    }

    public List<SolicitudInnova> misSolicitudes(String correo) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> solicitudes.listarPorCorreo(con, correo));
    }

    public List<SolicitudInnova> listarSolicitudes(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> solicitudes.listar(con, filtro));
    }

    /** Marca una solicitud PENDIENTE como ATENDIDA o RECHAZADA, con una observación para el solicitante. */
    public SolicitudInnova atender(String usuario, int id, String estado, String observacion) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            SolicitudInnova solicitud = solicitudes.buscar(con, id);
            if (solicitud == null) {
                throw new ErrorNegocio("No existe una solicitud con ID " + id + ".");
            }
            if (!solicitud.estado().equals("PENDIENTE")) {
                throw new ErrorNegocio("La solicitud " + id + " ya fue marcada como " + solicitud.estado() + ".");
            }
            solicitudes.atender(con, id, estado, observacion, usuario);
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "innova_solicitud", id,
                    estado + ": " + solicitud.nombre());
            return solicitud;
        });
    }
}
