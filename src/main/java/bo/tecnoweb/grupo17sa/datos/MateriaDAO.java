package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Materia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla materia (CU3). Todas las consultas usan PreparedStatement. */
public class MateriaDAO {

    private static final String COLUMNAS = "id, sigla, nombre, semestre, creditos, prerrequisito";

    public List<Materia> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM materia"
                + (todos ? "" : " WHERE sigla ILIKE ? OR nombre ILIKE ? OR prerrequisito ILIKE ?")
                + " ORDER BY semestre, sigla, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, Filtro.patron(filtro));
                ps.setString(2, Filtro.patron(filtro));
                ps.setString(3, Filtro.patron(filtro));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Materia> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Materia buscar(Connection con, int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM materia WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public Materia buscarPorSigla(Connection con, String sigla) throws SQLException {
        if (sigla == null || sigla.isBlank()) {
            return null;
        }
        String sql = "SELECT " + COLUMNAS + " FROM materia WHERE sigla ILIKE ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sigla.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, Materia m) throws SQLException {
        String sql = "INSERT INTO materia (sigla, nombre, semestre, creditos, prerrequisito) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.sigla().trim().toUpperCase());
            ps.setString(2, m.nombre().trim());
            ps.setInt(3, m.semestre());
            ps.setInt(4, m.creditos());
            ps.setString(5, m.prerrequisito() == null || m.prerrequisito().isBlank() ? null : m.prerrequisito().trim());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, Materia m) throws SQLException {
        String sql = "UPDATE materia SET sigla = ?, nombre = ?, semestre = ?, creditos = ?, prerrequisito = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.sigla().trim().toUpperCase());
            ps.setString(2, m.nombre().trim());
            ps.setInt(3, m.semestre());
            ps.setInt(4, m.creditos());
            ps.setString(5, m.prerrequisito() == null || m.prerrequisito().isBlank() ? null : m.prerrequisito().trim());
            ps.setInt(6, m.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        String sql = "DELETE FROM materia WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static Materia leer(ResultSet rs) throws SQLException {
        return new Materia(
                rs.getInt("id"),
                rs.getString("sigla"),
                rs.getString("nombre"),
                rs.getInt("semestre"),
                rs.getInt("creditos"),
                rs.getString("prerrequisito")
        );
    }
}
