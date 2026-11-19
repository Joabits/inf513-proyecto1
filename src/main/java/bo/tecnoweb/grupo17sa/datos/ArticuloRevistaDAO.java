package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.ArticuloRevista;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla revista_articulo (RF-08). */
public class ArticuloRevistaDAO {

    private static final String COLUMNAS = "id, edicion_id, titulo, autores, resumen, enlace_pdf, orden";

    public List<ArticuloRevista> listarDeEdicion(Connection con, int edicionId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT " + COLUMNAS + " FROM revista_articulo WHERE edicion_id = ? ORDER BY orden, id")) {
            ps.setInt(1, edicionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ArticuloRevista> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public ArticuloRevista buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM revista_articulo WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    /** El artículo nuevo va al final de la edición. */
    public int insertar(Connection con, ArticuloRevista a) throws SQLException {
        String sql = "INSERT INTO revista_articulo (edicion_id, titulo, autores, resumen, enlace_pdf, orden) VALUES (?, ?, ?, ?, ?,"
                + " (SELECT COALESCE(max(orden), 0) + 1 FROM revista_articulo WHERE edicion_id = ?)) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, a.edicionId());
            ps.setString(2, a.titulo());
            ps.setString(3, a.autores());
            ps.setString(4, a.resumen());
            ps.setString(5, a.enlacePdf());
            ps.setInt(6, a.edicionId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, ArticuloRevista a) throws SQLException {
        String sql = "UPDATE revista_articulo SET titulo = ?, autores = ?, resumen = ?, enlace_pdf = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.titulo());
            ps.setString(2, a.autores());
            ps.setString(3, a.resumen());
            ps.setString(4, a.enlacePdf());
            ps.setInt(5, a.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM revista_articulo WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static ArticuloRevista leer(ResultSet rs) throws SQLException {
        return new ArticuloRevista(rs.getInt("id"), rs.getInt("edicion_id"), rs.getString("titulo"), rs.getString("autores"),
                rs.getString("resumen"), rs.getString("enlace_pdf"), rs.getInt("orden"));
    }
}
