package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Galeria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla galeria (CU6). Todas las consultas usan PreparedStatement. */
public class GaleriaDAO {

    private static final String SELECT_BASE =
            "SELECT g.id, g.titulo, g.descripcion, g.imagen_url, g.evento_id, "
            + "e.titulo AS evento_nombre, g.orden, g.creado "
            + "FROM galeria g LEFT JOIN evento e ON g.evento_id = e.id";

    public List<Galeria> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = SELECT_BASE
                + (todos ? "" : " WHERE g.titulo ILIKE ? OR g.descripcion ILIKE ? OR e.titulo ILIKE ?")
                + " ORDER BY g.orden, g.id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                String p = Filtro.patron(filtro);
                ps.setString(1, p);
                ps.setString(2, p);
                ps.setString(3, p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Galeria> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Galeria buscar(Connection con, int id) throws SQLException {
        String sql = SELECT_BASE + " WHERE g.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, Galeria g) throws SQLException {
        String sql = "INSERT INTO galeria (titulo, descripcion, imagen_url, evento_id, orden) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, g.titulo().trim());
            ps.setString(2, g.descripcion() == null || g.descripcion().isBlank() ? null : g.descripcion().trim());
            ps.setString(3, g.imagenUrl().trim());
            if (g.eventoId() != null && g.eventoId() > 0) {
                ps.setInt(4, g.eventoId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setInt(5, g.orden());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, Galeria g) throws SQLException {
        String sql = "UPDATE galeria SET titulo = ?, descripcion = ?, imagen_url = ?, evento_id = ?, orden = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, g.titulo().trim());
            ps.setString(2, g.descripcion() == null || g.descripcion().isBlank() ? null : g.descripcion().trim());
            ps.setString(3, g.imagenUrl().trim());
            if (g.eventoId() != null && g.eventoId() > 0) {
                ps.setInt(4, g.eventoId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setInt(5, g.orden());
            ps.setInt(6, g.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        String sql = "DELETE FROM galeria WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static Galeria leer(ResultSet rs) throws SQLException {
        int evId = rs.getInt("evento_id");
        Integer eventoId = rs.wasNull() ? null : evId;
        return new Galeria(
                rs.getInt("id"),
                rs.getString("titulo"),
                rs.getString("descripcion"),
                rs.getString("imagen_url"),
                eventoId,
                rs.getString("evento_nombre"),
                rs.getInt("orden"),
                rs.getObject("creado", LocalDateTime.class)
        );
    }
}
