package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Banner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla banner (RF-01). Todas las consultas usan sentencias preparadas. */
public class BannerDAO {

    private static final String COLUMNAS = "id, titulo, subtitulo, imagen_url, enlace, orden";

    public List<Banner> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM banner"
                + (todos ? "" : " WHERE titulo ILIKE ? OR subtitulo ILIKE ?")
                + " ORDER BY orden, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, Filtro.patron(filtro));
                ps.setString(2, Filtro.patron(filtro));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Banner> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(leer(rs));
                }
                return lista;
            }
        }
    }

    public Banner buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM banner WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? leer(rs) : null;
            }
        }
    }

    public int insertar(Connection con, Banner b) throws SQLException {
        String sql = "INSERT INTO banner (titulo, subtitulo, imagen_url, enlace, orden) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.titulo());
            ps.setString(2, b.subtitulo());
            ps.setString(3, b.imagenUrl());
            ps.setString(4, b.enlace());
            ps.setInt(5, b.orden());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean modificar(Connection con, Banner b) throws SQLException {
        String sql = "UPDATE banner SET titulo = ?, subtitulo = ?, imagen_url = ?, enlace = ?, orden = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.titulo());
            ps.setString(2, b.subtitulo());
            ps.setString(3, b.imagenUrl());
            ps.setString(4, b.enlace());
            ps.setInt(5, b.orden());
            ps.setInt(6, b.id());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM banner WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static Banner leer(ResultSet rs) throws SQLException {
        return new Banner(rs.getInt("id"), rs.getString("titulo"), rs.getString("subtitulo"),
                rs.getString("imagen_url"), rs.getString("enlace"), rs.getInt("orden"));
    }
}
