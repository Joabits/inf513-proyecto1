package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.GraduadoDAO;
import bo.tecnoweb.grupo17sa.modelo.Graduado;

import java.sql.SQLException;
import java.time.Year;
import java.util.List;

/**
 * RF-11 Registro y seguimiento de graduados (Alumni). Cada graduado se registra y actualiza sus datos
 * desde su propio correo; el administrador consulta, filtra y exporta la información.
 */
public class AlumniNegocio {

    private static final String MODULO = "CU5";

    private final GraduadoDAO graduados = new GraduadoDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    public int registrar(Graduado graduado) throws SQLException, ErrorNegocio {
        validar(graduado);
        return Transaccion.ejecutar(con -> {
            if (graduados.buscarPorCorreo(con, graduado.correo()) != null) {
                throw new ErrorNegocio("El correo " + graduado.correo() + " ya está registrado. Para cambiar sus datos use ACTGRA.");
            }
            int id = graduados.insertar(con, graduado);
            bitacora.registrar(con, graduado.correo(), BitacoraNegocio.CREAR, MODULO, "graduado", id, nombreCompleto(graduado));
            return id;
        });
    }

    public void actualizar(Graduado graduado) throws SQLException, ErrorNegocio {
        validar(graduado);
        Transaccion.ejecutar(con -> {
            Graduado actual = graduados.buscarPorCorreo(con, graduado.correo());
            if (actual == null) {
                throw new ErrorNegocio("El correo " + graduado.correo() + " no está registrado. Primero regístrese con REGGRA.");
            }
            graduados.actualizarPorCorreo(con, graduado);
            bitacora.registrar(con, graduado.correo(), BitacoraNegocio.MODIFICAR, MODULO, "graduado", actual.id(),
                    nombreCompleto(graduado));
            return null;
        });
    }

    public Graduado miRegistro(String correo) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Graduado graduado = graduados.buscarPorCorreo(con, correo);
            if (graduado == null) {
                throw new ErrorNegocio("El correo " + correo + " no está registrado como graduado. Puede registrarse con REGGRA.");
            }
            return graduado;
        });
    }

    public List<Graduado> listar(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> graduados.listar(con, filtro));
    }

    public Graduado eliminar(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Graduado graduado = graduados.buscar(con, id);
            if (graduado == null) {
                throw new ErrorNegocio("No existe un graduado con ID " + id + ".");
            }
            graduados.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "graduado", id, nombreCompleto(graduado));
            return graduado;
        });
    }

    private static void validar(Graduado g) throws ErrorNegocio {
        int actual = Year.now().getValue();
        if (g.anioTitulacion() < 1960 || g.anioTitulacion() > actual) {
            throw new ErrorNegocio("El año de titulación debe estar entre 1960 y " + actual + ".");
        }
        boolean sinEmpleo = g.sector().equals("SIN_EMPLEO");
        if (!sinEmpleo && g.empleoActual() == null) {
            throw new ErrorNegocio("Indique dónde trabaja (empleo_actual) o use el sector SIN_EMPLEO.");
        }
        if (sinEmpleo && (g.empleoActual() != null || g.cargo() != null)) {
            throw new ErrorNegocio("Con el sector SIN_EMPLEO, empleo_actual y cargo deben ir vacíos (\"\").");
        }
    }

    private static String nombreCompleto(Graduado g) {
        return g.nombre() + " " + g.apellidos();
    }
}
