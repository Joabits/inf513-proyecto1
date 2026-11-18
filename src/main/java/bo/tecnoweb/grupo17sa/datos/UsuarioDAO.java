package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla usuario (RF-18). El correo es único y se guarda en minúsculas. */
public class UsuarioDAO {

    private static final String CONSULTA = "SELECT u.id, u.correo, u.nombre, u.rol_id, r.nombre AS rol, u.activo, u.creado"
            + " FROM usuario u JOIN rol r ON r.id = u.rol_id";

    public List<Usuario> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = CONSULTA + (todos ? "" : " WHERE u.correo ILIKE ? OR u.nombre ILIKE ? OR r.nombre ILIKE ?")
                + " ORDER BY u.activo DESC, u.nombre";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 3; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Usuario> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Usuario buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE u.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public Usuario buscarPorCorreo(Connection con, String correo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE u.correo = ?")) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, String correo, String nombre, int rolId) throws SQLException {
        String sql = "INSERT INTO usuario (correo, nombre, rol_id) VALUES (?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            ps.setString(2, nombre);
            ps.setInt(3, rolId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, int id, String nombre, int rolId, boolean activo) throws SQLException {
        String sql = "UPDATE usuario SET nombre = ?, rol_id = ?, activo = ?, actualizado = now() WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, rolId);
            ps.setBoolean(3, activo);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM usuario WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** ¿El correo es de un usuario activo cuyo rol tiene el caso de uso (o '*')? */
    public boolean tienePermiso(Connection con, String correo, String modulo) throws SQLException {
        String sql = "SELECT 1 FROM usuario u JOIN rol_modulo m ON m.rol_id = u.rol_id"
                + " WHERE u.correo = ? AND u.activo AND (m.modulo = '*' OR m.modulo = ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            ps.setString(2, modulo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Usuarios activos con acceso total ('*'), sin contar al indicado. */
    public int contarAdministradoresActivos(Connection con, int excluirId) throws SQLException {
        String sql = "SELECT count(DISTINCT u.id) FROM usuario u JOIN rol_modulo m ON m.rol_id = u.rol_id"
                + " WHERE u.activo AND m.modulo = '*' AND u.id <> ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static Usuario leer(ResultSet rs) throws SQLException {
        Timestamp creado = rs.getTimestamp("creado");
        return new Usuario(rs.getInt("id"), rs.getString("correo"), rs.getString("nombre"), rs.getInt("rol_id"),
                rs.getString("rol"), rs.getBoolean("activo"), creado == null ? null : creado.toLocalDateTime());
    }
}
