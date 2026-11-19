package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.ProyectoInvestigacion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla investigacion_proyecto (RF-07). */
public class ProyectoInvestigacionDAO {

    private static final String CONSULTA = "SELECT p.id, p.titulo, p.tipo, p.linea_id, l.nombre AS linea, p.responsable,"
            + " p.integrantes, p.gestion, p.estado, p.resumen, p.publicado, p.creado"
            + " FROM investigacion_proyecto p LEFT JOIN investigacion_linea l ON l.id = p.linea_id";

    /** "*" todos; otro texto busca en título, resumen, responsable, integrantes, tipo y línea. soloPublicados para el visitante. */
    public List<ProyectoInvestigacion> listar(Connection con, String filtro, boolean soloPublicados) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        List<String> condiciones = new ArrayList<>();
        if (soloPublicados) {
            condiciones.add("p.publicado");
        }
        if (!todos) {
            condiciones.add("(p.titulo ILIKE ? OR p.resumen ILIKE ? OR p.responsable ILIKE ? OR p.integrantes ILIKE ?"
                    + " OR p.tipo ILIKE ? OR l.nombre ILIKE ?)");
        }
        String sql = CONSULTA + (condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones))
                + " ORDER BY p.gestion DESC, p.titulo";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 6; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<ProyectoInvestigacion> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public ProyectoInvestigacion buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE p.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, ProyectoInvestigacion p) throws SQLException {
        String sql = "INSERT INTO investigacion_proyecto (titulo, tipo, linea_id, responsable, integrantes, gestion, estado, resumen)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            enlazar(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, ProyectoInvestigacion p) throws SQLException {
        String sql = "UPDATE investigacion_proyecto SET titulo = ?, tipo = ?, linea_id = ?, responsable = ?, integrantes = ?,"
                + " gestion = ?, estado = ?, resumen = ?, actualizado = now() WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            enlazar(ps, p);
            ps.setInt(9, p.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean publicar(Connection con, int id, boolean publicado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE investigacion_proyecto SET publicado = ? WHERE id = ?")) {
            ps.setBoolean(1, publicado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM investigacion_proyecto WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static void enlazar(PreparedStatement ps, ProyectoInvestigacion p) throws SQLException {
        ps.setString(1, p.titulo());
        ps.setString(2, p.tipo());
        if (p.lineaId() == null) {
            ps.setNull(3, Types.INTEGER);
        } else {
            ps.setInt(3, p.lineaId());
        }
        ps.setString(4, p.responsable());
        ps.setString(5, p.integrantes());
        ps.setInt(6, p.gestion());
        ps.setString(7, p.estado());
        ps.setString(8, p.resumen());
    }

    private static ProyectoInvestigacion leer(ResultSet rs) throws SQLException {
        int linea = rs.getInt("linea_id");
        Integer lineaId = rs.wasNull() ? null : linea;
        Timestamp creado = rs.getTimestamp("creado");
        return new ProyectoInvestigacion(rs.getInt("id"), rs.getString("titulo"), rs.getString("tipo"), lineaId,
                rs.getString("linea"), rs.getString("responsable"), rs.getString("integrantes"), rs.getInt("gestion"),
                rs.getString("estado"), rs.getString("resumen"), rs.getBoolean("publicado"),
                creado == null ? null : creado.toLocalDateTime());
    }
}
