package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.RedSocial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla red_social (RF-16). El nombre es único: repetirlo da error de unicidad. */
public class RedSocialDAO {

    private static final String COLUMNAS = "id, nombre, url, orden";

    public List<RedSocial> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM red_social"
                + (todos ? "" : " WHERE nombre ILIKE ? OR url ILIKE ?")
                + " ORDER BY orden, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, Filtro.patron(filtro));
                ps.setString(2, Filtro.patron(filtro));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<RedSocial> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public RedSocial buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM red_social WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, RedSocial r) throws SQLException {
        String sql = "INSERT INTO red_social (nombre, url, orden) VALUES (?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.nombre());
            ps.setString(2, r.url());
            ps.setInt(3, r.orden());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, RedSocial r) throws SQLException {
        String sql = "UPDATE red_social SET nombre = ?, url = ?, orden = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.nombre());
            ps.setString(2, r.url());
            ps.setInt(3, r.orden());
            ps.setInt(4, r.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM red_social WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static RedSocial leer(ResultSet rs) throws SQLException {
        return new RedSocial(rs.getInt("id"), rs.getString("nombre"), rs.getString("url"), rs.getInt("orden"));
    }
}
