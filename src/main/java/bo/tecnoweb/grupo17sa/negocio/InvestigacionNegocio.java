package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.LineaInvestigacionDAO;
import bo.tecnoweb.grupo17sa.datos.ProyectoInvestigacionDAO;
import bo.tecnoweb.grupo17sa.modelo.LineaInvestigacion;
import bo.tecnoweb.grupo17sa.modelo.ProyectoInvestigacion;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Year;
import java.util.List;

/** CU4 Líneas y proyectos de investigación (RF-07). */
public class InvestigacionNegocio {

    public static final String MODULO = "CU4";
    public static final int PRIMERA_GESTION = 1970;

    private final LineaInvestigacionDAO lineas = new LineaInvestigacionDAO();
    private final ProyectoInvestigacionDAO proyectos = new ProyectoInvestigacionDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    // ------------------------------------------------------------ Líneas

    public List<LineaInvestigacion> listarLineas(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> lineas.listar(con, filtro));
    }

    public int insertarLinea(String usuario, String nombre, String descripcion) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            int id = lineas.insertar(con, nombre.trim(), descripcion);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "investigacion_linea", id, nombre.trim());
            return id;
        });
    }

    public void modificarLinea(String usuario, int id, String nombre, String descripcion) throws SQLException, ErrorNegocio {
        Transaccion.ejecutar(con -> {
            if (!lineas.modificar(con, id, nombre.trim(), descripcion)) {
                throw new ErrorNegocio("No existe una línea de investigación con ID " + id + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "investigacion_linea", id, nombre.trim());
            return null;
        });
    }

    public LineaInvestigacion eliminarLinea(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            LineaInvestigacion linea = lineas.buscar(con, id);
            if (linea == null) {
                throw new ErrorNegocio("No existe una línea de investigación con ID " + id + ".");
            }
            int cantidad = lineas.contarProyectos(con, id);
            if (cantidad > 0) {
                throw new ErrorNegocio("La línea «" + linea.nombre() + "» tiene " + cantidad
                        + " proyecto(s). Cámbielos de línea con MODPRO antes de eliminarla.");
            }
            lineas.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "investigacion_linea", id, linea.nombre());
            return linea;
        });
    }

    // ------------------------------------------------------------ Proyectos

    public List<ProyectoInvestigacion> listarProyectos(String filtro, boolean verOcultos) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> proyectos.listar(con, filtro, !verOcultos));
    }

    /** Un proyecto oculto solo lo ve quien administra el CU4; para los demás no existe. */
    public ProyectoInvestigacion verProyecto(int id, boolean verOcultos) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            ProyectoInvestigacion p = proyectos.buscar(con, id);
            if (p == null || (!p.publicado() && !verOcultos)) {
                throw new ErrorNegocio("No existe un proyecto publicado con ID " + id + ".");
            }
            return p;
        });
    }

    public int insertarProyecto(String usuario, ProyectoInvestigacion p) throws SQLException, ErrorNegocio {
        validarGestion(p.gestion());
        return Transaccion.ejecutar(con -> {
            exigirLinea(con, p.lineaId());
            int id = proyectos.insertar(con, p);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "investigacion_proyecto", id, p.titulo());
            return id;
        });
    }

    public void modificarProyecto(String usuario, ProyectoInvestigacion p) throws SQLException, ErrorNegocio {
        validarGestion(p.gestion());
        Transaccion.ejecutar(con -> {
            exigirLinea(con, p.lineaId());
            if (!proyectos.modificar(con, p)) {
                throw new ErrorNegocio("No existe un proyecto con ID " + p.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "investigacion_proyecto", p.id(), p.titulo());
            return null;
        });
    }

    public ProyectoInvestigacion publicarProyecto(String usuario, int id, boolean publicado) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            ProyectoInvestigacion p = proyectoExistente(con, id);
            proyectos.publicar(con, id, publicado);
            bitacora.registrar(con, usuario, BitacoraNegocio.PUBLICAR, MODULO, "investigacion_proyecto", id,
                    (publicado ? "Publicado: " : "Oculto: ") + p.titulo());
            return p;
        });
    }

    public ProyectoInvestigacion eliminarProyecto(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            ProyectoInvestigacion p = proyectoExistente(con, id);
            proyectos.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "investigacion_proyecto", id, p.titulo());
            return p;
        });
    }

    // ------------------------------------------------------------ Reglas

    /** "" sin línea; un número debe ser el ID de una línea (LISLIN). */
    public static Integer leerLinea(String valor) throws ErrorNegocio {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new ErrorNegocio("La línea se indica con su ID (número de LISLIN) o vacía (\"\"); se recibió «" + valor + "».");
        }
    }

    private static void validarGestion(int gestion) throws ErrorNegocio {
        int maximo = Year.now().getValue() + 1;
        if (gestion < PRIMERA_GESTION || gestion > maximo) {
            throw new ErrorNegocio("La gestión debe estar entre " + PRIMERA_GESTION + " y " + maximo + ".");
        }
    }

    private void exigirLinea(Connection con, Integer lineaId) throws SQLException, ErrorNegocio {
        if (lineaId != null && lineas.buscar(con, lineaId) == null) {
            throw new ErrorNegocio("No existe una línea de investigación con ID " + lineaId + ". Envíe LISLIN[\"*\"] para verlas.");
        }
    }

    private ProyectoInvestigacion proyectoExistente(Connection con, int id) throws SQLException, ErrorNegocio {
        ProyectoInvestigacion p = proyectos.buscar(con, id);
        if (p == null) {
            throw new ErrorNegocio("No existe un proyecto con ID " + id + ".");
        }
        return p;
    }
}
