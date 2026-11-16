package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.SolicitudInnova;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla innova_solicitud (RF-10). */
public class SolicitudInnovaDAO {

    private static final String COLUMNAS = "id, tipo, nombre, correo, telefono, organizacion, mensaje, estado,"
            + " observacion, fecha, atendido_por, atendido_en";

    /** "*" todo; otro texto busca en tipo, nombre, correo, organización, mensaje y estado (por ejemplo "PENDIENTE"). */
    public List<SolicitudInnova> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM innova_solicitud"
                + (todos ? "" : " WHERE tipo ILIKE ? OR nombre ILIKE ? OR correo ILIKE ? OR organizacion ILIKE ?"
                + " OR mensaje ILIKE ? OR estado ILIKE ?")
                + " ORDER BY fecha DESC, id DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 6; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            return leerLista(ps);
        }
    }

    public List<SolicitudInnova> listarPorCorreo(Connection con, String correo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT " + COLUMNAS + " FROM innova_solicitud WHERE correo = ? ORDER BY fecha DESC, id DESC")) {
            ps.setString(1, correo);
            return leerLista(ps);
        }
    }

    public int contarPendientes(Connection con, String correo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT count(*) FROM innova_solicitud WHERE correo = ? AND estado = 'PENDIENTE'")) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public SolicitudInnova buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM innova_solicitud WHERE id = ?")) {
            ps.setInt(1, id);
            List<SolicitudInnova> lista = leerLista(ps);
            return lista.isEmpty() ? null : lista.get(0);
        }
    }

    public int insertar(Connection con, SolicitudInnova s) throws SQLException {
        String sql = "INSERT INTO innova_solicitud (tipo, nombre, correo, telefono, organizacion, mensaje)"
                + " VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.tipo());
            ps.setString(2, s.nombre());
            ps.setString(3, s.correo());
            ps.setString(4, s.telefono());
            ps.setString(5, s.organizacion());
            ps.setString(6, s.mensaje());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean atender(Connection con, int id, String estado, String observacion, String atendidoPor) throws SQLException {
        String sql = "UPDATE innova_solicitud SET estado = ?, observacion = ?, atendido_por = ?, atendido_en = now() WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, observacion);
            ps.setString(3, atendidoPor);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static List<SolicitudInnova> leerLista(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<SolicitudInnova> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new SolicitudInnova(rs.getInt("id"), rs.getString("tipo"), rs.getString("nombre"),
                        rs.getString("correo"), rs.getString("telefono"), rs.getString("organizacion"),
                        rs.getString("mensaje"), rs.getString("estado"), rs.getString("observacion"),
                        rs.getObject("fecha", LocalDateTime.class), rs.getString("atendido_por"),
                        rs.getObject("atendido_en", LocalDateTime.class)));
            }
            return lista;
        }
    }
}
