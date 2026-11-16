package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.Configuracion;

import java.util.Locale;

/**
 * Permisos para los comandos que no son públicos.
 *
 * TEMPORAL: mientras no esté integrado el CU1 (Gestión de Usuarios, roles y permisos), un correo
 * tiene permiso si figura en admin.correos de config.properties. Al integrar el CU1, este método
 * debe consultar el rol del usuario y sus permisos sobre el módulo en la base de datos.
 */
public class AccesoNegocio {

    public boolean tienePermiso(String correo, String modulo, String comando) {
        return correo != null && Configuracion.lista("admin.correos").contains(correo.toLowerCase(Locale.ROOT));
    }
}
