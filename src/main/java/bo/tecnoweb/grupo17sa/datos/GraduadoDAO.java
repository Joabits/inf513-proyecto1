package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Graduado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla graduado (RF-11). El correo es único: un registro por graduado. */
public class GraduadoDAO {

    private static final String COLUMNAS = "id, correo, nombre, apellidos, telefono, anio_titulacion, empleo_actual,"
            + " cargo, sector, ciudad, actualizado";

    /** "*" todo; otro texto busca en nombre, apellidos, correo, empleo, cargo, sector, ciudad y año de titulación. */
    public List<Graduado> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM graduado"
                + (todos ? "" : " WHERE nombre ILIKE ? OR apellidos ILIKE ? OR correo ILIKE ? OR empleo_actual ILIKE ?"
                + " OR cargo ILIKE ? OR sector ILIKE ? OR ciudad ILIKE ? OR anio_titulacion::text ILIKE ?")
                + " ORDER BY anio_titulacion DESC, apellidos, nombre";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 8; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            return leerLista(ps);
        }
    }

    public Graduado buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM graduado WHERE id = ?")) {
            ps.setInt(1, id);
            List<Graduado> lista = leerLista(ps);
            return lista.isEmpty() ? null : lista.get(0);
        }
    }

    public Graduado buscarPorCorreo(Connection con, String correo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM graduado WHERE correo = ?")) {
            ps.setString(1, correo);
            List<Graduado> lista = leerLista(ps);
            return lista.isEmpty() ? null : lista.get(0);
        }
    }

    public int insertar(Connection con, Graduado g) throws SQLException {
        String sql = "INSERT INTO graduado (correo, nombre, apellidos, telefono, anio_titulacion, empleo_actual, cargo,"
                + " sector, ciudad) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, g.correo());
            asignarDatos(ps, g, 2);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Actualiza los datos del graduado que tiene ese correo. */
    public boolean actualizarPorCorreo(Connection con, Graduado g) throws SQLException {
        String sql = "UPDATE graduado SET nombre = ?, apellidos = ?, telefono = ?, anio_titulacion = ?, empleo_actual = ?,"
                + " cargo = ?, sector = ?, ciudad = ?, actualizado = now() WHERE correo = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            asignarDatos(ps, g, 1);
            ps.setString(9, g.correo());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM graduado WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Nombre, apellidos, teléfono, año, empleo, cargo, sector y ciudad a partir de la posición "desde". */
    private static void asignarDatos(PreparedStatement ps, Graduado g, int desde) throws SQLException {
        ps.setString(desde, g.nombre());
        ps.setString(desde + 1, g.apellidos());
        ps.setString(desde + 2, g.telefono());
        ps.setInt(desde + 3, g.anioTitulacion());
        ps.setString(desde + 4, g.empleoActual());
        ps.setString(desde + 5, g.cargo());
        ps.setString(desde + 6, g.sector());
        ps.setString(desde + 7, g.ciudad());
    }

    private static List<Graduado> leerLista(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Graduado> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new Graduado(rs.getInt("id"), rs.getString("correo"), rs.getString("nombre"),
                        rs.getString("apellidos"), rs.getString("telefono"), rs.getInt("anio_titulacion"),
                        rs.getString("empleo_actual"), rs.getString("cargo"), rs.getString("sector"),
                        rs.getString("ciudad"), rs.getObject("actualizado", LocalDateTime.class)));
            }
            return lista;
        }
    }
}
