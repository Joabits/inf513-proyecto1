package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.EdicionRevista;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla revista_edicion (RF-08). El par (año, número) es único. */
public class EdicionRevistaDAO {

    private static final String CONSULTA = "SELECT e.id, e.volumen, e.numero, e.anio, e.titulo, e.descripcion, e.enlace_pdf,"
            + " e.publicada, (SELECT count(*) FROM revista_articulo a WHERE a.edicion_id = e.id) AS articulos"
            + " FROM revista_edicion e";

    /** "*" todas; otro texto busca en título, descripción y año. soloPublicadas para el visitante. */
    public List<EdicionRevista> listar(Connection con, String filtro, boolean soloPublicadas) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        List<String> condiciones = new ArrayList<>();
        if (soloPublicadas) {
            condiciones.add("e.publicada");
        }
        if (!todos) {
            condiciones.add("(e.titulo ILIKE ? OR e.descripcion ILIKE ? OR e.anio::text ILIKE ?)");
        }
        String sql = CONSULTA + (condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones))
                + " ORDER BY e.anio DESC, e.numero DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 3; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<EdicionRevista> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public EdicionRevista buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE e.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    /** Otra edición con el mismo año y número (para avisar antes de que falle la restricción de unicidad). */
    public EdicionRevista buscarPorNumero(Connection con, int anio, int numero) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + " WHERE e.anio = ? AND e.numero = ?")) {
            ps.setInt(1, anio);
            ps.setInt(2, numero);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, EdicionRevista e) throws SQLException {
        String sql = "INSERT INTO revista_edicion (volumen, numero, anio, titulo, descripcion, enlace_pdf)"
                + " VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            enlazar(ps, e);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, EdicionRevista e) throws SQLException {
        String sql = "UPDATE revista_edicion SET volumen = ?, numero = ?, anio = ?, titulo = ?, descripcion = ?, enlace_pdf = ?"
                + " WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            enlazar(ps, e);
            ps.setInt(7, e.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean publicar(Connection con, int id, boolean publicada) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE revista_edicion SET publicada = ? WHERE id = ?")) {
            ps.setBoolean(1, publicada);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Borra la edición; sus artículos se borran con ella (ON DELETE CASCADE). */
    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM revista_edicion WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static void enlazar(PreparedStatement ps, EdicionRevista e) throws SQLException {
        if (e.volumen() == null) {
            ps.setNull(1, Types.SMALLINT);
        } else {
            ps.setInt(1, e.volumen());
        }
        ps.setInt(2, e.numero());
        ps.setInt(3, e.anio());
        ps.setString(4, e.titulo());
        ps.setString(5, e.descripcion());
        ps.setString(6, e.enlacePdf());
    }

    private static EdicionRevista leer(ResultSet rs) throws SQLException {
        int volumen = rs.getInt("volumen");
        return new EdicionRevista(rs.getInt("id"), rs.wasNull() ? null : volumen, rs.getInt("numero"), rs.getInt("anio"),
                rs.getString("titulo"), rs.getString("descripcion"), rs.getString("enlace_pdf"), rs.getBoolean("publicada"),
                rs.getInt("articulos"));
    }
}
