package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.DocenteDAO;
import bo.tecnoweb.grupo17sa.datos.InfoAcademicaDAO;
import bo.tecnoweb.grupo17sa.datos.MateriaDAO;
import bo.tecnoweb.grupo17sa.modelo.Docente;
import bo.tecnoweb.grupo17sa.modelo.InfoAcademica;
import bo.tecnoweb.grupo17sa.modelo.Materia;

import java.sql.SQLException;
import java.util.List;

/**
 * CU3 Información Académica, Malla Curricular y Plantel Docente.
 * Todas las operaciones de escritura se ejecutan dentro de Transaccion.ejecutar(...)
 * y se registran en BitacoraNegocio usando la misma conexión.
 */
public class AcademicaNegocio {

    private static final String MODULO = "CU3";

    private final InfoAcademicaDAO infoDAO = new InfoAcademicaDAO();
    private final MateriaDAO materiaDAO = new MateriaDAO();
    private final DocenteDAO docenteDAO = new DocenteDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    // =========================================================================
    // Información Académica (info_academica)
    // =========================================================================

    public List<InfoAcademica> listarInfo(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> infoDAO.listar(con, filtro));
    }

    public InfoAcademica buscarInfo(int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> infoDAO.buscar(con, id));
    }

    public int insertarInfo(String usuario, InfoAcademica info) throws SQLException, ErrorNegocio {
        validarOrden(info.orden());
        return Transaccion.ejecutar(con -> {
            int id = infoDAO.insertar(con, info);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "info_academica", id, info.titulo());
            return id;
        });
    }

    public void modificarInfo(String usuario, InfoAcademica info) throws SQLException, ErrorNegocio {
        validarOrden(info.orden());
        Transaccion.ejecutar(con -> {
            if (!infoDAO.modificar(con, info)) {
                throw new ErrorNegocio("No existe la sección de información académica con ID " + info.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "info_academica", info.id(), info.titulo());
            return null;
        });
    }

    public InfoAcademica eliminarInfo(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            InfoAcademica existente = infoDAO.buscar(con, id);
            if (existente == null) {
                throw new ErrorNegocio("No existe la sección de información académica con ID " + id + ".");
            }
            infoDAO.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "info_academica", id, existente.titulo());
            return existente;
        });
    }

    // =========================================================================
    // Malla Curricular / Materias (materia)
    // =========================================================================

    public List<Materia> listarMaterias(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> materiaDAO.listar(con, filtro));
    }

    public Materia buscarMateria(int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> materiaDAO.buscar(con, id));
    }

    public int insertarMateria(String usuario, Materia m) throws SQLException, ErrorNegocio {
        validarMateria(m);
        return Transaccion.ejecutar(con -> {
            if (materiaDAO.buscarPorSigla(con, m.sigla()) != null) {
                throw new ErrorNegocio("Ya existe una materia con la sigla «" + m.sigla().trim().toUpperCase() + "».");
            }
            int id = materiaDAO.insertar(con, m);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "materia", id,
                    m.sigla().trim().toUpperCase() + " - " + m.nombre().trim());
            return id;
        });
    }

    public void modificarMateria(String usuario, Materia m) throws SQLException, ErrorNegocio {
        validarMateria(m);
        Transaccion.ejecutar(con -> {
            Materia existente = materiaDAO.buscar(con, m.id());
            if (existente == null) {
                throw new ErrorNegocio("No existe la materia con ID " + m.id() + ".");
            }
            Materia conMismaSigla = materiaDAO.buscarPorSigla(con, m.sigla());
            if (conMismaSigla != null && conMismaSigla.id() != m.id()) {
                throw new ErrorNegocio("Ya existe otra materia con la sigla «" + m.sigla().trim().toUpperCase() + "».");
            }
            materiaDAO.modificar(con, m);
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "materia", m.id(),
                    m.sigla().trim().toUpperCase() + " - " + m.nombre().trim());
            return null;
        });
    }

    public Materia eliminarMateria(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Materia existente = materiaDAO.buscar(con, id);
            if (existente == null) {
                throw new ErrorNegocio("No existe la materia con ID " + id + ".");
            }
            materiaDAO.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "materia", id,
                    existente.sigla() + " - " + existente.nombre());
            return existente;
        });
    }

    // =========================================================================
    // Plantel Docente (docente)
    // =========================================================================

    public List<Docente> listarDocentes(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> docenteDAO.listar(con, filtro));
    }

    public Docente buscarDocente(int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> docenteDAO.buscar(con, id));
    }

    public int insertarDocente(String usuario, Docente d) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            if (d.materiaId() != null && d.materiaId() > 0 && materiaDAO.buscar(con, d.materiaId()) == null) {
                throw new ErrorNegocio("No existe ninguna materia con ID " + d.materiaId() + ".");
            }
            int id = docenteDAO.insertar(con, d);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "docente", id, d.nombre().trim());
            return id;
        });
    }

    public void modificarDocente(String usuario, Docente d) throws SQLException, ErrorNegocio {
        Transaccion.ejecutar(con -> {
            Docente existente = docenteDAO.buscar(con, d.id());
            if (existente == null) {
                throw new ErrorNegocio("No existe el docente con ID " + d.id() + ".");
            }
            if (d.materiaId() != null && d.materiaId() > 0 && materiaDAO.buscar(con, d.materiaId()) == null) {
                throw new ErrorNegocio("No existe ninguna materia con ID " + d.materiaId() + ".");
            }
            docenteDAO.modificar(con, d);
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "docente", d.id(), d.nombre().trim());
            return null;
        });
    }

    public Docente eliminarDocente(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Docente existente = docenteDAO.buscar(con, id);
            if (existente == null) {
                throw new ErrorNegocio("No existe el docente con ID " + id + ".");
            }
            docenteDAO.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "docente", id, existente.nombre());
            return existente;
        });
    }

    // =========================================================================
    // Validaciones
    // =========================================================================

    private static void validarOrden(int orden) throws ErrorNegocio {
        if (orden <= 0) {
            throw new ErrorNegocio("El orden debe ser mayor a cero (se recibió " + orden + ").");
        }
    }

    private static void validarMateria(Materia m) throws ErrorNegocio {
        if (m.semestre() <= 0) {
            throw new ErrorNegocio("El semestre debe ser mayor a cero (se recibió " + m.semestre() + ").");
        }
        if (m.creditos() < 0) {
            throw new ErrorNegocio("Los créditos no pueden ser negativos (se recibió " + m.creditos() + ").");
        }
    }
}
