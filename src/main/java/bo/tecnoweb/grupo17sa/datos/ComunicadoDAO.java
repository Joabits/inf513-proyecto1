package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Comunicado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla comunicado (CU6). Todas las consultas usan PreparedStatement. */
public class ComunicadoDAO {

    private static final String COLUMNAS = "id, titulo, contenido, prioridad, fecha_publicacion, fecha_expiracion, creado";

    /** Lista comunicados vigentes (no expirados). */
    public List<Comunicado> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM comunicado WHERE (fecha_expiracion IS NULL OR fecha_expiracion >= now())"
                + (todos ? "" : " AND (titulo ILIKE ? OR contenido ILIKE ? OR prioridad ILIKE ?)")
                + " ORDER BY CASE prioridad WHEN 'URGENTE' THEN 1 WHEN 'ALTA' THEN 2 WHEN 'MEDIA' THEN 3 ELSE 4 END, fecha_publicacion DESC, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                String p = Filtro.patron(filtro);
                for (int i = 1; i <= 3; i++) {
                    ps.setString(i, p);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Comunicado> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    /** Lista todos los comunicados (vigentes o expirados) para administradores. */
    public List<Comunicado> listarTodos(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM comunicado"
                + (todos ? "" : " WHERE titulo ILIKE ? OR contenido ILIKE ? OR prioridad ILIKE ?")
                + " ORDER BY fecha_publicacion DESC, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                String p = Filtro.patron(filtro);
                for (int i = 1; i <= 3; i++) {
                    ps.setString(i, p);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Comunicado> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Comunicado buscar(Connection con, int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM comunicado WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, Comunicado c) throws SQLException {
        String sql = "INSERT INTO comunicado (titulo, contenido, prioridad, fecha_publicacion, fecha_expiracion) "
                + "VALUES (?, ?, ?, coalesce(?, now()), ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.titulo().trim());
            ps.setString(2, c.contenido().trim());
            ps.setString(3, c.prioridad().trim().toUpperCase());
            if (c.fechaPublicacion() != null) {
                ps.setObject(4, c.fechaPublicacion());
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }
            if (c.fechaExpiracion() != null) {
                ps.setObject(5, c.fechaExpiracion());
            } else {
                ps.setNull(5, Types.TIMESTAMP);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, Comunicado c) throws SQLException {
        String sql = "UPDATE comunicado SET titulo = ?, contenido = ?, prioridad = ?, fecha_expiracion = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.titulo().trim());
            ps.setString(2, c.contenido().trim());
            ps.setString(3, c.prioridad().trim().toUpperCase());
            if (c.fechaExpiracion() != null) {
                ps.setObject(4, c.fechaExpiracion());
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }
            ps.setInt(5, c.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        String sql = "DELETE FROM comunicado WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static Comunicado leer(ResultSet rs) throws SQLException {
        return new Comunicado(
                rs.getInt("id"),
                rs.getString("titulo"),
                rs.getString("contenido"),
                rs.getString("prioridad"),
                rs.getObject("fecha_publicacion", LocalDateTime.class),
                rs.getObject("fecha_expiracion", LocalDateTime.class),
                rs.getObject("creado", LocalDateTime.class)
        );
    }
}
