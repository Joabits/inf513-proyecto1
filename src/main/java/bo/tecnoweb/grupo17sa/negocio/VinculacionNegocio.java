package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.ConvenioDAO;
import bo.tecnoweb.grupo17sa.modelo.Convenio;

import java.sql.SQLException;
import java.util.List;

/** RF-09 Gestión de convenios y vinculación. */
public class VinculacionNegocio {

    private static final String MODULO = "CU5";

    private final ConvenioDAO convenios = new ConvenioDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    public List<Convenio> listar(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> convenios.listar(con, filtro));
    }

    public int insertar(String usuario, Convenio convenio) throws SQLException, ErrorNegocio {
        validar(convenio);
        return Transaccion.ejecutar(con -> {
            int id = convenios.insertar(con, convenio);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "convenio", id, convenio.institucion());
            return id;
        });
    }

    public void modificar(String usuario, Convenio convenio) throws SQLException, ErrorNegocio {
        validar(convenio);
        Transaccion.ejecutar(con -> {
            if (!convenios.modificar(con, convenio)) {
                throw new ErrorNegocio("No existe un convenio con ID " + convenio.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "convenio", convenio.id(), convenio.institucion());
            return null;
        });
    }

    public Convenio eliminar(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Convenio convenio = convenios.buscar(con, id);
            if (convenio == null) {
                throw new ErrorNegocio("No existe un convenio con ID " + id + ".");
            }
            convenios.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "convenio", id, convenio.institucion());
            return convenio;
        });
    }

    private static void validar(Convenio c) throws ErrorNegocio {
        if (c.fechaFin() != null && c.fechaFin().isBefore(c.fechaInicio())) {
            throw new ErrorNegocio("La fecha de fin (" + c.fechaFin() + ") no puede ser anterior a la de inicio ("
                    + c.fechaInicio() + ").");
        }
        if (c.estado().equals("CONCLUIDO") && c.fechaFin() == null) {
            throw new ErrorNegocio("Un convenio CONCLUIDO debe tener fecha de fin.");
        }
    }
}
