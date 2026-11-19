package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.Configuracion;
import bo.tecnoweb.grupo17sa.datos.UsuarioDAO;
import bo.tecnoweb.grupo17sa.modelo.Rol;
import bo.tecnoweb.grupo17sa.modelo.Usuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CU1 Gestión de Usuarios (RF-18). Un usuario se identifica por el correo desde el que envía los comandos y tiene uno de
 * los tres roles fijos (Rol): Administrador, Editor institucional o Editor de comunicación. Los roles no se crean ni se
 * modifican; quien no es usuario activo es Visitante. AccesoNegocio consulta estos datos para cada comando privado.
 */
public class UsuariosNegocio {

    public static final String MODULO = "CU1";

    /** Lo que muestra MISROL: el usuario (o null si es Visitante) y si es administrador de arranque. */
    public record MiCuenta(String correo, Usuario usuario, boolean administradorArranque) {
    }

    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    public List<Usuario> listarUsuarios(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> usuarios.listar(con, filtro));
    }

    public int insertarUsuario(String autor, String correo, String nombre, String textoRol) throws SQLException, ErrorNegocio {
        String limpio = correo.trim().toLowerCase(Locale.ROOT);
        Rol rol = leerRol(textoRol);
        return Transaccion.ejecutar(con -> {
            Usuario existente = usuarios.buscarPorCorreo(con, limpio);
            if (existente != null) {
                throw new ErrorNegocio("Ya existe un usuario con el correo " + limpio + " (ID " + existente.id() + ").");
            }
            int id = usuarios.insertar(con, limpio, nombre, rol);
            bitacora.registrar(con, autor, BitacoraNegocio.CREAR, MODULO, "usuario", id, limpio + " (" + rol.name() + ")");
            return id;
        });
    }

    public void modificarUsuario(String autor, int id, String nombre, String textoRol, boolean activo)
            throws SQLException, ErrorNegocio {
        Rol rol = leerRol(textoRol);
        Transaccion.ejecutar(con -> {
            Usuario usuario = usuarioExistente(con, id);
            boolean cambiaRol = rol != usuario.rol();
            if (esElMismo(autor, usuario) && (cambiaRol || !activo)) {
                throw new ErrorNegocio("No puede desactivarse ni cambiar su propio rol; pídaselo a otro administrador.");
            }
            if ((cambiaRol && rol != Rol.ADMINISTRADOR) || !activo) {
                exigirOtroAdministrador(con, usuario);
            }
            usuarios.modificar(con, id, nombre, rol, activo);
            bitacora.registrar(con, autor, BitacoraNegocio.MODIFICAR, MODULO, "usuario", id,
                    usuario.correo() + " (" + rol.name() + ", " + (activo ? "activo" : "inactivo") + ")");
            return null;
        });
    }

    public Usuario eliminarUsuario(String autor, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Usuario usuario = usuarioExistente(con, id);
            if (esElMismo(autor, usuario)) {
                throw new ErrorNegocio("No puede eliminar su propio usuario; pídaselo a otro administrador.");
            }
            exigirOtroAdministrador(con, usuario);
            usuarios.eliminar(con, id);
            bitacora.registrar(con, autor, BitacoraNegocio.ELIMINAR, MODULO, "usuario", id, usuario.correo());
            return usuario;
        });
    }

    public MiCuenta miCuenta(String correo) throws SQLException, ErrorNegocio {
        String limpio = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        boolean arranque = Configuracion.lista("admin.correos").contains(limpio);
        return Transaccion.ejecutar(con -> new MiCuenta(limpio, usuarios.buscarPorCorreo(con, limpio), arranque));
    }

    /** Los tres roles fijos con su cantidad de usuarios. */
    public Map<Rol, Integer> listarRoles() throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(usuarios::contarPorRol);
    }

    /** El rol que escribió el administrador, o un error con los tres roles válidos. */
    public static Rol leerRol(String texto) throws ErrorNegocio {
        Rol rol = Rol.leer(texto);
        if (rol == null) {
            throw new ErrorNegocio("El rol «" + texto.trim() + "» no existe. Los roles son fijos: "
                    + "ADMINISTRADOR, EDITOR_INSTITUCIONAL o EDITOR_COMUNICACION.");
        }
        return rol;
    }

    private Usuario usuarioExistente(Connection con, int id) throws SQLException, ErrorNegocio {
        Usuario usuario = usuarios.buscar(con, id);
        if (usuario == null) {
            throw new ErrorNegocio("No existe un usuario con ID " + id + ".");
        }
        return usuario;
    }

    private static boolean esElMismo(String autor, Usuario usuario) {
        return autor != null && autor.trim().equalsIgnoreCase(usuario.correo());
    }

    /**
     * Si el usuario es administrador activo, debe quedar otro (o un administrador de arranque en config.properties):
     * si no, nadie podría volver a gestionar los usuarios.
     */
    private void exigirOtroAdministrador(Connection con, Usuario usuario) throws SQLException, ErrorNegocio {
        boolean esAdministrador = usuario.activo() && usuario.rol() == Rol.ADMINISTRADOR;
        if (esAdministrador && usuarios.contarAdministradoresActivos(con, usuario.id()) == 0
                && Configuracion.lista("admin.correos").isEmpty()) {
            throw new ErrorNegocio(usuario.correo() + " es el único administrador activo: registre otro antes de quitarle el acceso.");
        }
    }
}
