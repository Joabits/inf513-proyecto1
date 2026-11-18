package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Documento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla documento (RF-06). */
public class DocumentoDAO {

    private static final String COLUMNAS = "id, titulo, descripcion, categoria, enlace, publicado, creado";

    /** "*" todos; otro texto busca en título, descripción y categoría. soloPublicados para el visitante. */
    public List<Documento> listar(Connection con, String filtro, boolean soloPublicados) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        List<String> condiciones = new ArrayList<>();
        if (soloPublicados) {
            condiciones.add("publicado");
        }
        if (!todos) {
            condiciones.add("(titulo ILIKE ? OR descripcion ILIKE ? OR categoria ILIKE ?)");
        }
        String sql = "SELECT " + COLUMNAS + " FROM documento"
                + (condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones)) + " ORDER BY categoria, titulo";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 3; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Documento> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Documento buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM documento WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, Documento d) throws SQLException {
        String sql = "INSERT INTO documento (titulo, descripcion, categoria, enlace) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, d.titulo());
            ps.setString(2, d.descripcion());
            ps.setString(3, d.categoria());
            ps.setString(4, d.enlace());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, Documento d) throws SQLException {
        String sql = "UPDATE documento SET titulo = ?, descripcion = ?, categoria = ?, enlace = ?, actualizado = now() WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, d.titulo());
            ps.setString(2, d.descripcion());
            ps.setString(3, d.categoria());
            ps.setString(4, d.enlace());
            ps.setInt(5, d.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean publicar(Connection con, int id, boolean publicado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE documento SET publicado = ? WHERE id = ?")) {
            ps.setBoolean(1, publicado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM documento WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static Documento leer(ResultSet rs) throws SQLException {
        Timestamp creado = rs.getTimestamp("creado");
        return new Documento(rs.getInt("id"), rs.getString("titulo"), rs.getString("descripcion"), rs.getString("categoria"),
                rs.getString("enlace"), rs.getBoolean("publicado"), creado == null ? null : creado.toLocalDateTime());
    }
}
