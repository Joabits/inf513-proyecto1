package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.LineaInvestigacion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla investigacion_linea (RF-07). El nombre es único. */
public class LineaInvestigacionDAO {

    private static final String CONSULTA = "SELECT l.id, l.nombre, l.descripcion,"
            + " (SELECT count(*) FROM investigacion_proyecto p WHERE p.linea_id = l.id AND p.publicado) AS proyectos"
            + " FROM investigacion_linea l";

    public List<LineaInvestigacion> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = CONSULTA + (todos ? "" : " WHERE l.nombre ILIKE ? OR l.descripcion ILIKE ?") + " ORDER BY l.id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, Filtro.patron(filtro));
                ps.setString(2, Filtro.patron(filtro));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<LineaInvestigacion> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public LineaInvestigacion buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE l.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, String nombre, String descripcion) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO investigacion_linea (nombre, descripcion) VALUES (?, ?) RETURNING id")) {
            ps.setString(1, nombre);
            ps.setString(2, descripcion);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, int id, String nombre, String descripcion) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE investigacion_linea SET nombre = ?, descripcion = ? WHERE id = ?")) {
            ps.setString(1, nombre);
            ps.setString(2, descripcion);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM investigacion_linea WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Todos los proyectos de la línea, publicados o no (un proyecto oculto también impide borrarla). */
    public int contarProyectos(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT count(*) FROM investigacion_proyecto WHERE linea_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static LineaInvestigacion leer(ResultSet rs) throws SQLException {
        return new LineaInvestigacion(rs.getInt("id"), rs.getString("nombre"), rs.getString("descripcion"), rs.getInt("proyectos"));
    }
}
