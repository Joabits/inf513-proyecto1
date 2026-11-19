package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.Configuracion;
import bo.tecnoweb.grupo17sa.Registro;
import bo.tecnoweb.grupo17sa.datos.ConexionDB;
import bo.tecnoweb.grupo17sa.datos.UsuarioDAO;
import bo.tecnoweb.grupo17sa.modelo.Usuario;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Permisos para los comandos que no son públicos (CU1, RF-18).
 *
 * Un correo puede usar un comando privado si es un usuario activo y su rol fijo lo permite (Rol.puede). Si no, es
 * Visitante y solo usa los comandos públicos. Los correos de admin.correos (config.properties) son administradores de
 * arranque: tienen permiso aunque no estén en la base, para poder registrar el primer usuario con INSUSU.
 */
public class AccesoNegocio {

    private final UsuarioDAO usuarios = new UsuarioDAO();

    public boolean tienePermiso(String correo, String modulo, String comando) {
        if (correo == null || correo.isBlank()) {
            return false;
        }
        String limpio = correo.trim().toLowerCase(Locale.ROOT);
        if (Configuracion.lista("admin.correos").contains(limpio)) {
            return true;
        }
        if (Configuracion.texto("db.url").isEmpty()) {
            return false;
        }
        try (Connection con = ConexionDB.obtener()) {
            Usuario usuario = usuarios.buscarPorCorreo(con, limpio);
            return usuario != null && usuario.activo() && usuario.rol().puede(modulo, comando);
        } catch (SQLException e) {
            // 42P01: el CU1 todavía no está instalado; solo valen los administradores de arranque
            if (!"42P01".equals(e.getSQLState())) {
                Registro.error("No se pudo verificar el permiso de " + limpio + " sobre " + modulo + " (" + comando + ")", e);
            }
            return false;
        }
    }
}
