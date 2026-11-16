package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Convenio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla convenio (RF-09). */
public class ConvenioDAO {

    private static final String COLUMNAS = "id, tipo, institucion, objeto, fecha_inicio, fecha_fin, estado, responsable, logo_url";

    public List<Convenio> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM convenio"
                + (todos ? "" : " WHERE tipo ILIKE ? OR institucion ILIKE ? OR objeto ILIKE ? OR estado ILIKE ? OR responsable ILIKE ?")
                + " ORDER BY fecha_inicio DESC, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 5; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Convenio> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Convenio buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM convenio WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, Convenio c) throws SQLException {
        String sql = "INSERT INTO convenio (tipo, institucion, objeto, fecha_inicio, fecha_fin, estado, responsable, logo_url)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            asignar(ps, c);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, Convenio c) throws SQLException {
        String sql = "UPDATE convenio SET tipo = ?, institucion = ?, objeto = ?, fecha_inicio = ?, fecha_fin = ?,"
                + " estado = ?, responsable = ?, logo_url = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            asignar(ps, c);
            ps.setInt(9, c.id());
            return ps.executeUpdate() > 0;
        }
    }

    /** Convenios VIGENTES cuya fecha de fin ya pasó o llega dentro de los próximos "dias" días. */
    public List<Convenio> vigentesPorVencer(Connection con, int dias) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM convenio WHERE estado = 'VIGENTE' AND fecha_fin IS NOT NULL"
                + " AND fecha_fin <= current_date + ? ORDER BY fecha_fin";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, dias);
            try (ResultSet rs = ps.executeQuery()) {
                List<Convenio> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM convenio WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static void asignar(PreparedStatement ps, Convenio c) throws SQLException {
        ps.setString(1, c.tipo());
        ps.setString(2, c.institucion());
        ps.setString(3, c.objeto());
        ps.setObject(4, c.fechaInicio());
        ps.setObject(5, c.fechaFin(), Types.DATE);
        ps.setString(6, c.estado());
        ps.setString(7, c.responsable());
        ps.setString(8, c.logoUrl());
    }

    private static Convenio leer(ResultSet rs) throws SQLException {
        return new Convenio(rs.getInt("id"), rs.getString("tipo"), rs.getString("institucion"), rs.getString("objeto"),
                rs.getObject("fecha_inicio", LocalDate.class), rs.getObject("fecha_fin", LocalDate.class),
                rs.getString("estado"), rs.getString("responsable"), rs.getString("logo_url"));
    }
}
