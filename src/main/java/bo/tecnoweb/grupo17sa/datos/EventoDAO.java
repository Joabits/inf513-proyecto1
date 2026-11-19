package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Evento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla evento (CU6). Todas las consultas usan PreparedStatement. */
public class EventoDAO {

    private static final String COLUMNAS = "id, titulo, descripcion, fecha_inicio, fecha_fin, lugar, imagen_url, creado";

    public List<Evento> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM evento"
                + (todos ? "" : " WHERE titulo ILIKE ? OR descripcion ILIKE ? OR lugar ILIKE ?")
                + " ORDER BY fecha_inicio DESC, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                String p = Filtro.patron(filtro);
                ps.setString(1, p);
                ps.setString(2, p);
                ps.setString(3, p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Evento> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Evento buscar(Connection con, int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM evento WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, Evento e) throws SQLException {
        String sql = "INSERT INTO evento (titulo, descripcion, fecha_inicio, fecha_fin, lugar, imagen_url) "
                + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.titulo().trim());
            ps.setString(2, e.descripcion().trim());
            ps.setObject(3, e.fechaInicio());
            if (e.fechaFin() != null) {
                ps.setObject(4, e.fechaFin());
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }
            ps.setString(5, e.lugar().trim());
            ps.setString(6, e.imagenUrl() == null || e.imagenUrl().isBlank() ? null : e.imagenUrl().trim());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, Evento e) throws SQLException {
        String sql = "UPDATE evento SET titulo = ?, descripcion = ?, fecha_inicio = ?, fecha_fin = ?, lugar = ?, imagen_url = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.titulo().trim());
            ps.setString(2, e.descripcion().trim());
            ps.setObject(3, e.fechaInicio());
            if (e.fechaFin() != null) {
                ps.setObject(4, e.fechaFin());
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }
            ps.setString(5, e.lugar().trim());
            ps.setString(6, e.imagenUrl() == null || e.imagenUrl().isBlank() ? null : e.imagenUrl().trim());
            ps.setInt(7, e.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        String sql = "DELETE FROM evento WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static Evento leer(ResultSet rs) throws SQLException {
        return new Evento(
                rs.getInt("id"),
                rs.getString("titulo"),
                rs.getString("descripcion"),
                rs.getObject("fecha_inicio", LocalDateTime.class),
                rs.getObject("fecha_fin", LocalDateTime.class),
                rs.getString("lugar"),
                rs.getString("imagen_url"),
                rs.getObject("creado", LocalDateTime.class)
        );
    }
}
