package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.MiembroInnova;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla innova_miembro (RF-10). */
public class MiembroInnovaDAO {

    private static final String COLUMNAS = "id, nombre, cargo, semestre, correo";

    public List<MiembroInnova> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM innova_miembro"
                + (todos ? "" : " WHERE nombre ILIKE ? OR cargo ILIKE ? OR semestre ILIKE ?")
                + " ORDER BY id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 3; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<MiembroInnova> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public MiembroInnova buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM innova_miembro WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, MiembroInnova m) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO innova_miembro (nombre, cargo, semestre, correo) VALUES (?, ?, ?, ?) RETURNING id")) {
            ps.setString(1, m.nombre());
            ps.setString(2, m.cargo());
            ps.setString(3, m.semestre());
            ps.setString(4, m.correo());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, MiembroInnova m) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE innova_miembro SET nombre = ?, cargo = ?, semestre = ?, correo = ? WHERE id = ?")) {
            ps.setString(1, m.nombre());
            ps.setString(2, m.cargo());
            ps.setString(3, m.semestre());
            ps.setString(4, m.correo());
            ps.setInt(5, m.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM innova_miembro WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static MiembroInnova leer(ResultSet rs) throws SQLException {
        return new MiembroInnova(rs.getInt("id"), rs.getString("nombre"), rs.getString("cargo"),
                rs.getString("semestre"), rs.getString("correo"));
    }
}
