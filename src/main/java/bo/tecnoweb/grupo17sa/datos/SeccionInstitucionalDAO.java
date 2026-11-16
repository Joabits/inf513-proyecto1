package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.SeccionInstitucional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla seccion_institucional (RF-02). Las secciones son fijas: solo se modifican. */
public class SeccionInstitucionalDAO {

    private static final String COLUMNAS = "clave, titulo, contenido, orden, actualizado";

    public List<SeccionInstitucional> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM seccion_institucional"
                + (todos ? "" : " WHERE clave ILIKE ? OR titulo ILIKE ? OR contenido ILIKE ?")
                + " ORDER BY orden, clave";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 3; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<SeccionInstitucional> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public SeccionInstitucional buscar(Connection con, String clave) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM seccion_institucional WHERE clave = ?")) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public boolean modificar(Connection con, String clave, String titulo, String contenido) throws SQLException {
        String sql = "UPDATE seccion_institucional SET titulo = ?, contenido = ?, actualizado = now() WHERE clave = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, titulo);
            ps.setString(2, contenido);
            ps.setString(3, clave);
            return ps.executeUpdate() > 0;
        }
    }

    private static SeccionInstitucional leer(ResultSet rs) throws SQLException {
        return new SeccionInstitucional(rs.getString("clave"), rs.getString("titulo"), rs.getString("contenido"),
                rs.getInt("orden"), rs.getObject("actualizado", LocalDateTime.class));
    }
}
