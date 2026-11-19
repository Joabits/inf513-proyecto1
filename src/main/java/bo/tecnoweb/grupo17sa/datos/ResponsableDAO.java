package bo.tecnoweb.grupo17sa.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * Columna usuario_id (FK a usuario) de las tablas de CU2 a CU7: el usuario que creó o modificó por última vez
 * cada registro. La asigna BitacoraNegocio en la misma transacción en que registra la acción.
 * Si el remitente no está registrado en la tabla usuario (por ejemplo, un administrador de arranque o una persona
 * que usa un comando público), queda en NULL.
 */
public class ResponsableDAO {

    /** Subconsulta que convierte el correo del remitente en el id de su usuario (o NULL si no está registrado). */
    public static final String ID_USUARIO = "(SELECT id FROM usuario WHERE correo = lower(?))";

    /**
     * Tablas que tienen usuario_id y el nombre de su clave primaria. El nombre de la tabla de la sentencia sale
     * siempre de esta lista, nunca de un dato del correo. bitacora no está: guarda su usuario_id al insertarse.
     */
    private static final Map<String, String> TABLAS = Map.ofEntries(
            Map.entry("configuracion", "clave"),
            Map.entry("seccion_institucional", "clave"),
            Map.entry("banner", "id"),
            Map.entry("destacado", "id"),
            Map.entry("acceso_rapido", "id"),
            Map.entry("red_social", "id"),
            Map.entry("info_academica", "id"),
            Map.entry("materia", "id"),
            Map.entry("docente", "id"),
            Map.entry("investigacion_linea", "id"),
            Map.entry("investigacion_proyecto", "id"),
            Map.entry("documento", "id"),
            Map.entry("revista_edicion", "id"),
            Map.entry("revista_articulo", "id"),
            Map.entry("convenio", "id"),
            Map.entry("innova_servicio", "id"),
            Map.entry("innova_miembro", "id"),
            Map.entry("innova_proyecto", "id"),
            Map.entry("innova_solicitud", "id"),
            Map.entry("graduado", "id"),
            Map.entry("noticia", "id"),
            Map.entry("comunicado", "id"),
            Map.entry("evento", "id"),
            Map.entry("galeria", "id"),
            Map.entry("mensaje_contacto", "id"));

    public static Set<String> tablas() {
        return TABLAS.keySet();
    }

    /**
     * Guarda en el registro {@code elemento} de {@code tabla} el usuario cuyo correo es {@code correo}.
     * No hace nada si la tabla no tiene usuario_id.
     */
    public void asignar(Connection con, String tabla, String elemento, String correo) throws SQLException {
        String clave = TABLAS.get(tabla);
        if (clave == null || elemento == null) {
            return;
        }
        String sql = "UPDATE " + tabla + " SET usuario_id = " + ID_USUARIO + " WHERE " + clave + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            if (clave.equals("id")) {
                ps.setInt(2, Integer.parseInt(elemento));
            } else {
                ps.setString(2, elemento);
            }
            ps.executeUpdate();
        }
    }
}
