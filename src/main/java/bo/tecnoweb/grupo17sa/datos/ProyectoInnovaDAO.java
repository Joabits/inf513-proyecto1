package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.ProyectoInnova;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla innova_proyecto (RF-10). */
public class ProyectoInnovaDAO {

    private static final String COLUMNAS = "id, nombre, cliente, descripcion, anio, resultado";

    public List<ProyectoInnova> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM innova_proyecto"
                + (todos ? "" : " WHERE nombre ILIKE ? OR cliente ILIKE ? OR descripcion ILIKE ? OR anio::text ILIKE ?")
                + " ORDER BY anio DESC, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 4; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<ProyectoInnova> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public ProyectoInnova buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM innova_proyecto WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, ProyectoInnova p) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO innova_proyecto (nombre, cliente, descripcion, anio, resultado) VALUES (?, ?, ?, ?, ?) RETURNING id")) {
            asignar(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, ProyectoInnova p) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE innova_proyecto SET nombre = ?, cliente = ?, descripcion = ?, anio = ?, resultado = ? WHERE id = ?")) {
            asignar(ps, p);
            ps.setInt(6, p.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM innova_proyecto WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static void asignar(PreparedStatement ps, ProyectoInnova p) throws SQLException {
        ps.setString(1, p.nombre());
        ps.setString(2, p.cliente());
        ps.setString(3, p.descripcion());
        ps.setInt(4, p.anio());
        ps.setString(5, p.resultado());
    }

    private static ProyectoInnova leer(ResultSet rs) throws SQLException {
        return new ProyectoInnova(rs.getInt("id"), rs.getString("nombre"), rs.getString("cliente"),
                rs.getString("descripcion"), rs.getInt("anio"), rs.getString("resultado"));
    }
}
