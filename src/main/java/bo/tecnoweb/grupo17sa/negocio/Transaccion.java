package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.ConexionDB;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ejecución transaccional: la operación y su registro en la bitácora se guardan juntos (COMMIT)
 * o no se guarda nada (ROLLBACK) si ocurre un error.
 */
public final class Transaccion {

    @FunctionalInterface
    public interface Operacion<T> {
        T ejecutar(Connection con) throws SQLException, ErrorNegocio;
    }

    private Transaccion() {
    }

    public static <T> T ejecutar(Operacion<T> operacion) throws SQLException, ErrorNegocio {
        try (Connection con = ConexionDB.obtener()) {
            con.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(con);
                con.commit();
                return resultado;
            } catch (SQLException | ErrorNegocio | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }
}
