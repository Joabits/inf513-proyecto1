package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Rol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Acceso a las tablas rol y rol_modulo (RF-18). El nombre del rol es único. */
public class RolDAO {

    /** El rol con sus casos de uso (separados por coma) y cuántos usuarios lo tienen. */
    private static final String CONSULTA = "SELECT r.id, r.nombre, r.descripcion,"
            + " COALESCE((SELECT string_agg(m.modulo, ',' ORDER BY m.modulo) FROM rol_modulo m WHERE m.rol_id = r.id), '') AS modulos,"
            + " (SELECT count(*) FROM usuario u WHERE u.rol_id = r.id) AS usuarios"
            + " FROM rol r";

    public List<Rol> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = CONSULTA + (todos ? "" : " WHERE r.nombre ILIKE ? OR r.descripcion ILIKE ?") + " ORDER BY r.id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, Filtro.patron(filtro));
                ps.setString(2, Filtro.patron(filtro));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Rol> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Rol buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE r.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public Rol buscarPorNombre(Connection con, String nombre) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE r.nombre = ?")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, String nombre, String descripcion) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO rol (nombre, descripcion) VALUES (?, ?) RETURNING id")) {
            ps.setString(1, nombre);
            ps.setString(2, descripcion);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, int id, String nombre, String descripcion) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE rol SET nombre = ?, descripcion = ? WHERE id = ?")) {
            ps.setString(1, nombre);
            ps.setString(2, descripcion);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Reemplaza los casos de uso del rol por los indicados. */
    public void reemplazarModulos(Connection con, int rolId, List<String> modulos) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM rol_modulo WHERE rol_id = ?")) {
            ps.setInt(1, rolId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO rol_modulo (rol_id, modulo) VALUES (?, ?)")) {
            for (String modulo : modulos) {
                ps.setInt(1, rolId);
                ps.setString(2, modulo);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM rol WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static Rol leer(ResultSet rs) throws SQLException {
        String modulos = rs.getString("modulos");
        return new Rol(rs.getInt("id"), rs.getString("nombre"), rs.getString("descripcion"),
                modulos.isEmpty() ? List.of() : Arrays.asList(modulos.split(",")), rs.getInt("usuarios"));
    }
}
