package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Rol;
import bo.tecnoweb.grupo17sa.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a la tabla usuario (RF-18). El correo es único y se guarda en minúsculas; la columna rol guarda el nombre de
 * uno de los tres roles fijos (ADMINISTRADOR, EDITOR_INSTITUCIONAL o EDITOR_COMUNICACION).
 */
public class UsuarioDAO {

    private static final String CONSULTA = "SELECT id, correo, nombre, rol, activo, creado FROM usuario";

    public List<Usuario> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = CONSULTA + (todos ? "" : " WHERE correo ILIKE ? OR nombre ILIKE ? OR rol ILIKE ?")
                + " ORDER BY activo DESC, nombre";
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
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public Usuario buscarPorCorreo(Connection con, String correo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE correo = ?")) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, String correo, String nombre, Rol rol) throws SQLException {
        String sql = "INSERT INTO usuario (correo, nombre, rol) VALUES (?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            ps.setString(2, nombre);
            ps.setString(3, rol.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, int id, String nombre, Rol rol, boolean activo) throws SQLException {
        String sql = "UPDATE usuario SET nombre = ?, rol = ?, activo = ?, actualizado = now() WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, rol.name());
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

    /** Correos de los usuarios activos que tienen alguno de los roles indicados (para los avisos por correo). */
    public List<String> correosConRol(Connection con, List<Rol> roles) throws SQLException {
        String[] nombres = new String[roles.size()];
        for (int i = 0; i < roles.size(); i++) {
            nombres[i] = roles.get(i).name();
        }
        String sql = "SELECT correo FROM usuario WHERE activo AND rol = ANY (?) ORDER BY correo";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setArray(1, con.createArrayOf("varchar", nombres));
            try (ResultSet rs = ps.executeQuery()) {
                List<String> correos = new ArrayList<>();
                while (rs.next()) {
                    correos.add(rs.getString(1));
                }
                return correos;
            }
        }
    }

    /** Cantidad de usuarios de cada rol (los roles sin usuarios quedan con 0). */
    public Map<Rol, Integer> contarPorRol(Connection con) throws SQLException {
        Map<Rol, Integer> cantidades = new EnumMap<>(Rol.class);
        for (Rol rol : Rol.values()) {
            cantidades.put(rol, 0);
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT rol, count(*) FROM usuario GROUP BY rol");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cantidades.put(Rol.valueOf(rs.getString(1)), rs.getInt(2));
            }
        }
        return cantidades;
    }

    /** Administradores activos, sin contar al indicado. */
    public int contarAdministradoresActivos(Connection con, int excluirId) throws SQLException {
        String sql = "SELECT count(*) FROM usuario WHERE activo AND rol = ? AND id <> ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, Rol.ADMINISTRADOR.name());
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static Usuario leer(ResultSet rs) throws SQLException {
        Timestamp creado = rs.getTimestamp("creado");
        return new Usuario(rs.getInt("id"), rs.getString("correo"), rs.getString("nombre"),
                Rol.valueOf(rs.getString("rol")), rs.getBoolean("activo"), creado == null ? null : creado.toLocalDateTime());
    }
}
