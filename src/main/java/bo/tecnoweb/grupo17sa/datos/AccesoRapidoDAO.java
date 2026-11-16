package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.AccesoRapido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla acceso_rapido (RF-01). */
public class AccesoRapidoDAO {

    private static final String COLUMNAS = "id, nombre, url, descripcion, orden";

    public List<AccesoRapido> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM acceso_rapido"
                + (todos ? "" : " WHERE nombre ILIKE ? OR descripcion ILIKE ?")
                + " ORDER BY orden, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, Filtro.patron(filtro));
                ps.setString(2, Filtro.patron(filtro));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<AccesoRapido> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public AccesoRapido buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM acceso_rapido WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, AccesoRapido a) throws SQLException {
        String sql = "INSERT INTO acceso_rapido (nombre, url, descripcion, orden) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.nombre());
            ps.setString(2, a.url());
            ps.setString(3, a.descripcion());
            ps.setInt(4, a.orden());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, AccesoRapido a) throws SQLException {
        String sql = "UPDATE acceso_rapido SET nombre = ?, url = ?, descripcion = ?, orden = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.nombre());
            ps.setString(2, a.url());
            ps.setString(3, a.descripcion());
            ps.setInt(4, a.orden());
            ps.setInt(5, a.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM acceso_rapido WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static AccesoRapido leer(ResultSet rs) throws SQLException {
        return new AccesoRapido(rs.getInt("id"), rs.getString("nombre"), rs.getString("url"),
                rs.getString("descripcion"), rs.getInt("orden"));
    }
}
