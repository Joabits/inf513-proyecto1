package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.ServicioInnova;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla innova_servicio (RF-10). */
public class ServicioInnovaDAO {

    public List<ServicioInnova> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT id, nombre, descripcion FROM innova_servicio"
                + (todos ? "" : " WHERE nombre ILIKE ? OR descripcion ILIKE ?")
                + " ORDER BY id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, Filtro.patron(filtro));
                ps.setString(2, Filtro.patron(filtro));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<ServicioInnova> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public ServicioInnova buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id, nombre, descripcion FROM innova_servicio WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, ServicioInnova s) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO innova_servicio (nombre, descripcion) VALUES (?, ?) RETURNING id")) {
            ps.setString(1, s.nombre());
            ps.setString(2, s.descripcion());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, ServicioInnova s) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE innova_servicio SET nombre = ?, descripcion = ? WHERE id = ?")) {
            ps.setString(1, s.nombre());
            ps.setString(2, s.descripcion());
            ps.setInt(3, s.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM innova_servicio WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static ServicioInnova leer(ResultSet rs) throws SQLException {
        return new ServicioInnova(rs.getInt("id"), rs.getString("nombre"), rs.getString("descripcion"));
    }
}
