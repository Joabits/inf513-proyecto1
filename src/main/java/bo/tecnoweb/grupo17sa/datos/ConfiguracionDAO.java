package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.DatoConfiguracion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla configuracion: datos de bienvenida (RF-01) y de contacto (RF-16). */
public class ConfiguracionDAO {

    private static final String COLUMNAS = "clave, valor, grupo, descripcion";

    public List<DatoConfiguracion> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM configuracion"
                + (todos ? "" : " WHERE clave ILIKE ? OR grupo ILIKE ? OR descripcion ILIKE ? OR valor ILIKE ?")
                + " ORDER BY grupo, clave";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 4; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            return leerLista(ps);
        }
    }

    public List<DatoConfiguracion> listarGrupo(Connection con, String grupo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM configuracion WHERE grupo = ? ORDER BY clave")) {
            ps.setString(1, grupo);
            return leerLista(ps);
        }
    }

    /** Valor de una clave, o "" si no existe. */
    public String valor(Connection con, String clave) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT valor FROM configuracion WHERE clave = ?")) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "";
            }
        }
    }

    public boolean modificar(Connection con, String clave, String valor) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE configuracion SET valor = ?, actualizado = now() WHERE clave = ?")) {
            ps.setString(1, valor);
            ps.setString(2, clave);
            return ps.executeUpdate() > 0;
        }
    }

    private static List<DatoConfiguracion> leerLista(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<DatoConfiguracion> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new DatoConfiguracion(rs.getString("clave"), rs.getString("valor"),
                        rs.getString("grupo"), rs.getString("descripcion")));
            }
            return lista;
        }
    }
}
