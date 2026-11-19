package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.DocumentoDAO;
import bo.tecnoweb.grupo17sa.modelo.Documento;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** CU4 Repositorio de documentos (RF-06): reglamentos, resoluciones, formularios... con el enlace a su archivo. */
public class DocumentosNegocio {

    private static final String MODULO = InvestigacionNegocio.MODULO;

    private final DocumentoDAO documentos = new DocumentoDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    public List<Documento> listar(String filtro, boolean verOcultos) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> documentos.listar(con, filtro, !verOcultos));
    }

    public int insertar(String usuario, Documento d) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            int id = documentos.insertar(con, d);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "documento", id, d.titulo());
            return id;
        });
    }

    public void modificar(String usuario, Documento d) throws SQLException, ErrorNegocio {
        Transaccion.ejecutar(con -> {
            if (!documentos.modificar(con, d)) {
                throw new ErrorNegocio("No existe un documento con ID " + d.id() + ".");
            }
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "documento", d.id(), d.titulo());
            return null;
        });
    }

    public Documento publicar(String usuario, int id, boolean publicado) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Documento d = existente(con, id);
            documentos.publicar(con, id, publicado);
            bitacora.registrar(con, usuario, BitacoraNegocio.PUBLICAR, MODULO, "documento", id,
                    (publicado ? "Publicado: " : "Oculto: ") + d.titulo());
            return d;
        });
    }

    public Documento eliminar(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Documento d = existente(con, id);
            documentos.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "documento", id, d.titulo());
            return d;
        });
    }

    private Documento existente(Connection con, int id) throws SQLException, ErrorNegocio {
        Documento d = documentos.buscar(con, id);
        if (d == null) {
            throw new ErrorNegocio("No existe un documento con ID " + id + ".");
        }
        return d;
    }
}
