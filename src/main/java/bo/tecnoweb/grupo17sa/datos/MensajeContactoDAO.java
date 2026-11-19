package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.MensajeContacto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Acceso a la tabla mensaje_contacto (RF-15). */
public class MensajeContactoDAO {

    public static final Set<String> ESTADOS = Set.of("NUEVO", "LEIDO", "RESPONDIDO", "ARCHIVADO");

    private static final String COLUMNAS = "id, correo, nombre, telefono, asunto, mensaje, estado, respuesta,"
            + " respondido_por, respondido_en, fecha";

    /** "*" todos; un estado (NUEVO, LEIDO, RESPONDIDO, ARCHIVADO) los de ese estado; otro texto busca en los datos. */
    public List<MensajeContacto> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String estado = todos ? null : filtro.trim().toUpperCase(Locale.ROOT).replace("Í", "I");
        boolean porEstado = estado != null && ESTADOS.contains(estado);
        String condicion = todos ? "" : porEstado ? " WHERE estado = ?"
                : " WHERE nombre ILIKE ? OR correo ILIKE ? OR asunto ILIKE ? OR mensaje ILIKE ?";
        String sql = "SELECT " + COLUMNAS + " FROM mensaje_contacto" + condicion + " ORDER BY fecha DESC, id DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (porEstado) {
                ps.setString(1, estado);
            } else if (!todos) {
                for (int i = 1; i <= 4; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            return leerTodos(ps);
        }
    }

    public List<MensajeContacto> listarPorCorreo(Connection con, String correo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT " + COLUMNAS + " FROM mensaje_contacto WHERE correo = ? ORDER BY fecha DESC, id DESC")) {
            ps.setString(1, correo);
            return leerTodos(ps);
        }
    }

    public MensajeContacto buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM mensaje_contacto WHERE id = ?")) {
            ps.setInt(1, id);
            List<MensajeContacto> lista = leerTodos(ps);
            return lista.isEmpty() ? null : lista.get(0);
        }
    }

    /** Mensajes del correo que todavía no fueron respondidos ni archivados. */
    public int contarPendientes(Connection con, String correo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT count(*) FROM mensaje_contacto WHERE correo = ? AND estado IN ('NUEVO', 'LEIDO')")) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int insertar(Connection con, MensajeContacto m) throws SQLException {
        String sql = "INSERT INTO mensaje_contacto (correo, nombre, telefono, asunto, mensaje) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.correo());
            ps.setString(2, m.nombre());
            ps.setString(3, m.telefono());
            ps.setString(4, m.asunto());
            ps.setString(5, m.mensaje());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean cambiarEstado(Connection con, int id, String estado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE mensaje_contacto SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean responder(Connection con, int id, String respuesta, String usuario) throws SQLException {
        String sql = "UPDATE mensaje_contacto SET estado = 'RESPONDIDO', respuesta = ?, respondido_por = ?, respondido_en = now()"
                + " WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, respuesta);
            ps.setString(2, usuario);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM mensaje_contacto WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static List<MensajeContacto> leerTodos(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<MensajeContacto> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new MensajeContacto(rs.getInt("id"), rs.getString("correo"), rs.getString("nombre"),
                        rs.getString("telefono"), rs.getString("asunto"), rs.getString("mensaje"), rs.getString("estado"),
                        rs.getString("respuesta"), rs.getString("respondido_por"), fecha(rs.getTimestamp("respondido_en")),
                        fecha(rs.getTimestamp("fecha"))));
            }
            return lista;
        }
    }

    private static LocalDateTime fecha(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
