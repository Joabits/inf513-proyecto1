package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.InfoAcademica;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla info_academica (CU3). Todas las consultas usan PreparedStatement. */
public class InfoAcademicaDAO {

    private static final String COLUMNAS = "id, titulo, contenido, orden, actualizado";

    public List<InfoAcademica> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM info_academica"
                + (todos ? "" : " WHERE titulo ILIKE ? OR contenido ILIKE ?")
                + " ORDER BY orden, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, Filtro.patron(filtro));
                ps.setString(2, Filtro.patron(filtro));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<InfoAcademica> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public InfoAcademica buscar(Connection con, int id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM info_academica WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, InfoAcademica info) throws SQLException {
        String sql = "INSERT INTO info_academica (titulo, contenido, orden, actualizado) VALUES (?, ?, ?, now()) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, info.titulo());
            ps.setString(2, info.contenido());
            ps.setInt(3, info.orden());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, InfoAcademica info) throws SQLException {
        String sql = "UPDATE info_academica SET titulo = ?, contenido = ?, orden = ?, actualizado = now() WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, info.titulo());
            ps.setString(2, info.contenido());
            ps.setInt(3, info.orden());
            ps.setInt(4, info.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        String sql = "DELETE FROM info_academica WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static InfoAcademica leer(ResultSet rs) throws SQLException {
        return new InfoAcademica(
                rs.getInt("id"),
                rs.getString("titulo"),
                rs.getString("contenido"),
                rs.getInt("orden"),
                rs.getObject("actualizado", LocalDateTime.class)
        );
    }
}
