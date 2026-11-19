package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Docente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla docente (CU3). Todas las consultas usan PreparedStatement. */
public class DocenteDAO {

    private static final String SELECT_BASE =
            "SELECT d.id, d.nombre, d.titulo, d.cargo, d.correo, d.materia_id, "
            + "CASE WHEN m.id IS NOT NULL THEN m.sigla || ' - ' || m.nombre ELSE NULL END AS materia_nombre "
            + "FROM docente d LEFT JOIN materia m ON d.materia_id = m.id";

    public List<Docente> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = SELECT_BASE
                + (todos ? "" : " WHERE d.nombre ILIKE ? OR d.titulo ILIKE ? OR d.cargo ILIKE ? OR d.correo ILIKE ? "
                + "OR m.sigla ILIKE ? OR m.nombre ILIKE ?")
                + " ORDER BY d.nombre, d.id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                String p = Filtro.patron(filtro);
                for (int i = 1; i <= 6; i++) {
                    ps.setString(i, p);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Docente> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Docente buscar(Connection con, int id) throws SQLException {
        String sql = SELECT_BASE + " WHERE d.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, Docente d) throws SQLException {
        String sql = "INSERT INTO docente (nombre, titulo, cargo, correo, materia_id) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, d.nombre().trim());
            ps.setString(2, d.titulo() == null || d.titulo().isBlank() ? null : d.titulo().trim());
            ps.setString(3, d.cargo() == null || d.cargo().isBlank() ? null : d.cargo().trim());
            ps.setString(4, d.correo() == null || d.correo().isBlank() ? null : d.correo().trim().toLowerCase());
            if (d.materiaId() != null && d.materiaId() > 0) {
                ps.setInt(5, d.materiaId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, Docente d) throws SQLException {
        String sql = "UPDATE docente SET nombre = ?, titulo = ?, cargo = ?, correo = ?, materia_id = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, d.nombre().trim());
            ps.setString(2, d.titulo() == null || d.titulo().isBlank() ? null : d.titulo().trim());
            ps.setString(3, d.cargo() == null || d.cargo().isBlank() ? null : d.cargo().trim());
            ps.setString(4, d.correo() == null || d.correo().isBlank() ? null : d.correo().trim().toLowerCase());
            if (d.materiaId() != null && d.materiaId() > 0) {
                ps.setInt(5, d.materiaId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setInt(6, d.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        String sql = "DELETE FROM docente WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static Docente leer(ResultSet rs) throws SQLException {
        int matId = rs.getInt("materia_id");
        Integer materiaId = rs.wasNull() ? null : matId;
        return new Docente(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("titulo"),
                rs.getString("cargo"),
                rs.getString("correo"),
                materiaId,
                rs.getString("materia_nombre")
        );
    }
}
