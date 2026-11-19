package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.RegistroBitacora;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla bitacora (RF-19). */
public class BitacoraDAO {

    /** Las consultas devuelven como máximo esta cantidad de registros, del más reciente al más antiguo. */
    public static final int MAXIMO = 200;

    private static final String COLUMNAS = "id, fecha, usuario, accion, modulo, tabla, elemento, detalle";

    public void registrar(Connection con, String usuario, String accion, String modulo, String tabla,
                          String elemento, String detalle) throws SQLException {
        // usuario guarda el correo tal como llegó (también el de los administradores de arranque);
        // usuario_id es la FK al usuario registrado con ese correo, o NULL si no está en la tabla usuario.
        String sql = "INSERT INTO bitacora (usuario, accion, modulo, tabla, elemento, detalle, usuario_id)"
                + " VALUES (?, ?, ?, ?, ?, ?, " + ResponsableDAO.ID_USUARIO + ")";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, accion);
            ps.setString(3, modulo);
            ps.setString(4, tabla);
            ps.setString(5, elemento);
            ps.setString(6, detalle);
            ps.setString(7, usuario);
            ps.executeUpdate();
        }
    }

    /** "*" devuelve todo; otro texto busca en usuario, acción, módulo, tabla y detalle. */
    public List<RegistroBitacora> listar(Connection con, String filtro) throws SQLException {
        boolean todos = Filtro.todos(filtro);
        String sql = "SELECT " + COLUMNAS + " FROM bitacora"
                + (todos ? "" : " WHERE usuario ILIKE ? OR accion ILIKE ? OR modulo ILIKE ? OR tabla ILIKE ? OR detalle ILIKE ?")
                + " ORDER BY fecha DESC, id DESC LIMIT " + MAXIMO;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (!todos) {
                for (int i = 1; i <= 5; i++) {
                    ps.setString(i, Filtro.patron(filtro));
                }
            }
            return leerLista(ps);
        }
    }

    /** Registros de un día. */
    public List<RegistroBitacora> listarDia(Connection con, LocalDate dia) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM bitacora WHERE fecha >= ? AND fecha < ?"
                + " ORDER BY fecha DESC, id DESC LIMIT " + MAXIMO;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, dia.atStartOfDay());
            ps.setObject(2, dia.plusDays(1).atStartOfDay());
            return leerLista(ps);
        }
    }

    private static List<RegistroBitacora> leerLista(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<RegistroBitacora> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new RegistroBitacora(rs.getLong("id"), rs.getObject("fecha", LocalDateTime.class),
                        rs.getString("usuario"), rs.getString("accion"), rs.getString("modulo"),
                        rs.getString("tabla"), rs.getString("elemento"), rs.getString("detalle")));
            }
            return lista;
        }
    }
}
