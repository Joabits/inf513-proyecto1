package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Noticia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla noticia (CU6). Todas las consultas usan PreparedStatement. */
public class NoticiaDAO {

    private static final String COLUMNAS = "id, titulo, resumen, contenido, imagen_url, autor, fecha_publicacion, publicado, orden, creado";

    /** Lista noticias públicas (publicado = true). */
    public List<Noticia> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM noticia WHERE publicado = true"
                + (todos ? "" : " AND (titulo ILIKE ? OR resumen ILIKE ? OR contenido ILIKE ? OR autor ILIKE ?)")
                + " ORDER BY orden, fecha_publicacion DESC, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                String p = Filtro.patron(filtro);
                for (int i = 1; i <= 4; i++) {
                    ps.setString(i, p);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Noticia> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    /** Lista todas las noticias (publicadas o no) para usuarios administradores. */
    public List<Noticia> listarTodas(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM noticia"
                + (todos ? "" : " WHERE titulo ILIKE ? OR resumen ILIKE ? OR contenido ILIKE ? OR autor ILIKE ?")
                + " ORDER BY orden, fecha_publicacion DESC, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                String p = Filtro.patron(filtro);
                for (int i = 1; i <= 4; i++) {
                    ps.setString(i, p);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Noticia> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Noticia buscar(Connection con, int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM noticia WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, Noticia n) throws SQLException {
        String sql = "INSERT INTO noticia (titulo, resumen, contenido, imagen_url, autor, fecha_publicacion, publicado, orden) "
                + "VALUES (?, ?, ?, ?, ?, coalesce(?, now()), ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, n.titulo().trim());
            ps.setString(2, n.resumen().trim());
            ps.setString(3, n.contenido().trim());
            ps.setString(4, n.imagenUrl() == null || n.imagenUrl().isBlank() ? null : n.imagenUrl().trim());
            ps.setString(5, n.autor().trim());
            if (n.fechaPublicacion() != null) {
                ps.setObject(6, n.fechaPublicacion());
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }
            ps.setBoolean(7, n.publicado());
            ps.setInt(8, n.orden());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, Noticia n) throws SQLException {
        String sql = "UPDATE noticia SET titulo = ?, resumen = ?, contenido = ?, imagen_url = ?, autor = ?, orden = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, n.titulo().trim());
            ps.setString(2, n.resumen().trim());
            ps.setString(3, n.contenido().trim());
            ps.setString(4, n.imagenUrl() == null || n.imagenUrl().isBlank() ? null : n.imagenUrl().trim());
            ps.setString(5, n.autor().trim());
            ps.setInt(6, n.orden());
            ps.setInt(7, n.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        String sql = "DELETE FROM noticia WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static Noticia leer(ResultSet rs) throws SQLException {
        return new Noticia(
                rs.getInt("id"),
                rs.getString("titulo"),
                rs.getString("resumen"),
                rs.getString("contenido"),
                rs.getString("imagen_url"),
                rs.getString("autor"),
                rs.getObject("fecha_publicacion", LocalDateTime.class),
                rs.getBoolean("publicado"),
                rs.getInt("orden"),
                rs.getObject("creado", LocalDateTime.class)
        );
    }
}
