package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Destacado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla destacado (RF-01). */
public class DestacadoDAO {

    private static final String COLUMNAS = "id, titulo, descripcion, enlace, orden";

    public List<Destacado> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM destacado"
                + (todos ? "" : " WHERE titulo ILIKE ? OR descripcion ILIKE ?")
                + " ORDER BY orden, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, Filtro.patron(filtro));
                ps.setString(2, Filtro.patron(filtro));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Destacado> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Destacado buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM destacado WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, Destacado d) throws SQLException {
        String sql = "INSERT INTO destacado (titulo, descripcion, enlace, orden) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, d.titulo());
            ps.setString(2, d.descripcion());
            ps.setString(3, d.enlace());
            ps.setInt(4, d.orden());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, Destacado d) throws SQLException {
        String sql = "UPDATE destacado SET titulo = ?, descripcion = ?, enlace = ?, orden = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, d.titulo());
            ps.setString(2, d.descripcion());
            ps.setString(3, d.enlace());
            ps.setInt(4, d.orden());
            ps.setInt(5, d.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM destacado WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static Destacado leer(ResultSet rs) throws SQLException {
        return new Destacado(rs.getInt("id"), rs.getString("titulo"), rs.getString("descripcion"),
                rs.getString("enlace"), rs.getInt("orden"));
    }
}
