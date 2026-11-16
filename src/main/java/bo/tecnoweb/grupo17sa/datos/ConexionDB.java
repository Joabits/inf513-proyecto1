package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.Configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Conexión a PostgreSQL con los datos de config.properties (db.url, db.usuario, db.clave). */
public final class ConexionDB {

    private ConexionDB() {
    }

    public static Connection obtener() throws SQLException {
        return DriverManager.getConnection(Configuracion.texto("db.url"),
                Configuracion.texto("db.usuario"), Configuracion.texto("db.clave"));
    }

    /** Por ejemplo "db_grupo17sa como grupo17sa (PostgreSQL 18.3)"; lanza la excepción si no conecta. */
    public static String probar() throws SQLException {
        try (Connection con = obtener();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT current_database(), current_user, current_setting('server_version')")) {
            rs.next();
            return rs.getString(1) + " como " + rs.getString(2) + " (PostgreSQL " + rs.getString(3) + ")";
        }
    }
}
