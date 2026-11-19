package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.Configuracion;
import bo.tecnoweb.grupo17sa.datos.RolDAO;
import bo.tecnoweb.grupo17sa.datos.UsuarioDAO;
import bo.tecnoweb.grupo17sa.modelo.Rol;
import bo.tecnoweb.grupo17sa.modelo.Usuario;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * CU1 Gestión de Usuarios, roles y permisos (RF-18). Un usuario se identifica por el correo desde el que envía los
 * comandos; su rol dice qué casos de uso puede administrar. AccesoNegocio consulta estos datos para cada comando privado.
 */
public class UsuariosNegocio {

    public static final String MODULO = "CU1";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String TODOS = "*";
    public static final List<String> CASOS_DE_USO = List.of("CU1", "CU2", "CU3", "CU4", "CU5", "CU6", "CU7", "CU8");

    private static final Pattern NOMBRE_ROL = Pattern.compile("[A-Z][A-Z0-9_]{2,39}");

    /** Lo que muestra MISROL: el usuario (o null si no está registrado) y si es administrador de arranque. */
    public record MiCuenta(String correo, Usuario usuario, Rol rol, boolean administradorArranque) {
    }

    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final RolDAO roles = new RolDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    // ------------------------------------------------------------ Usuarios

    public List<Usuario> listarUsuarios(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> usuarios.listar(con, filtro));
    }

    public int insertarUsuario(String autor, String correo, String nombre, String nombreRol) throws SQLException, ErrorNegocio {
        String limpio = correo.trim().toLowerCase(Locale.ROOT);
        return Transaccion.ejecutar(con -> {
            Rol rol = rolExistente(con, nombreRol);
            Usuario existente = usuarios.buscarPorCorreo(con, limpio);
            if (existente != null) {
                throw new ErrorNegocio("Ya existe un usuario con el correo " + limpio + " (ID " + existente.id() + ").");
            }
            int id = usuarios.insertar(con, limpio, nombre, rol.id());
            bitacora.registrar(con, autor, BitacoraNegocio.CREAR, MODULO, "usuario", id, limpio + " (" + rol.nombre() + ")");
            return id;
        });
    }

    public void modificarUsuario(String autor, int id, String nombre, String nombreRol, boolean activo)
            throws SQLException, ErrorNegocio {
        Transaccion.ejecutar(con -> {
            Usuario usuario = usuarioExistente(con, id);
            Rol rol = rolExistente(con, nombreRol);
            boolean cambiaRol = rol.id() != usuario.rolId();
            if (esElMismo(autor, usuario) && (cambiaRol || !activo)) {
                throw new ErrorNegocio("No puede desactivarse ni cambiar su propio rol; pídaselo a otro administrador.");
            }
            if ((cambiaRol && !rol.modulos().contains(TODOS)) || !activo) {
                exigirOtroAdministrador(con, usuario);
            }
            usuarios.modificar(con, id, nombre, rol.id(), activo);
            bitacora.registrar(con, autor, BitacoraNegocio.MODIFICAR, MODULO, "usuario", id,
                    usuario.correo() + " (" + rol.nombre() + ", " + (activo ? "activo" : "inactivo") + ")");
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
        return Transaccion.ejecutar(con -> {
            Usuario usuario = usuarios.buscarPorCorreo(con, limpio);
            Rol rol = usuario == null ? null : roles.buscar(con, usuario.rolId());
            return new MiCuenta(limpio, usuario, rol, arranque);
        });
    }

    // ------------------------------------------------------------ Roles

    public List<Rol> listarRoles(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> roles.listar(con, filtro));
    }

    public int insertarRol(String autor, String nombre, String descripcion, String casosDeUso) throws SQLException, ErrorNegocio {
        String limpio = normalizarRol(nombre);
        List<String> modulos = leerCasosDeUso(casosDeUso);
        return Transaccion.ejecutar(con -> {
            if (roles.buscarPorNombre(con, limpio) != null) {
                throw new ErrorNegocio("Ya existe el rol " + limpio + ".");
            }
            int id = roles.insertar(con, limpio, descripcion);
            roles.reemplazarModulos(con, id, modulos);
            bitacora.registrar(con, autor, BitacoraNegocio.CREAR, MODULO, "rol", id, limpio + ": " + String.join(",", modulos));
            return id;
        });
    }

    public void modificarRol(String autor, int id, String nombre, String descripcion, String casosDeUso)
            throws SQLException, ErrorNegocio {
        String limpio = normalizarRol(nombre);
        List<String> modulos = leerCasosDeUso(casosDeUso);
        Transaccion.ejecutar(con -> {
            Rol rol = rolExistente(con, id);
            if (rol.nombre().equals(ADMINISTRADOR) && (!limpio.equals(ADMINISTRADOR) || !modulos.contains(TODOS))) {
                throw new ErrorNegocio("El rol ADMINISTRADOR no se puede renombrar ni quitarle el acceso total (*).");
            }
            Rol otro = roles.buscarPorNombre(con, limpio);
            if (otro != null && otro.id() != id) {
                throw new ErrorNegocio("Ya existe otro rol llamado " + limpio + " (ID " + otro.id() + ").");
            }
            roles.modificar(con, id, limpio, descripcion);
            roles.reemplazarModulos(con, id, modulos);
            bitacora.registrar(con, autor, BitacoraNegocio.MODIFICAR, MODULO, "rol", id, limpio + ": " + String.join(",", modulos));
            return null;
        });
    }

    public Rol eliminarRol(String autor, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Rol rol = rolExistente(con, id);
            if (rol.nombre().equals(ADMINISTRADOR)) {
                throw new ErrorNegocio("El rol ADMINISTRADOR no se puede eliminar.");
            }
            if (rol.usuarios() > 0) {
                throw new ErrorNegocio("El rol " + rol.nombre() + " tiene " + rol.usuarios()
                        + " usuario(s). Cámbielos de rol con MODUSU antes de eliminarlo.");
            }
            roles.eliminar(con, id);
            bitacora.registrar(con, autor, BitacoraNegocio.ELIMINAR, MODULO, "rol", id, rol.nombre());
            return rol;
        });
    }

    // ------------------------------------------------------------ Reglas

    /** «editor de prensa» → «EDITOR_DE_PRENSA». Solo letras sin tilde, números y guion bajo. */
    public static String normalizarRol(String nombre) throws ErrorNegocio {
        String sinTildes = Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String limpio = sinTildes.toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
        if (!NOMBRE_ROL.matcher(limpio).matches()) {
            throw new ErrorNegocio("El nombre del rol debe tener de 3 a 40 letras, números o guiones bajos, por ejemplo EDITOR_PRENSA.");
        }
        return limpio;
    }

    /** «CU2, cu4» → [CU2, CU4]; «*» → [*]. */
    public static List<String> leerCasosDeUso(String texto) throws ErrorNegocio {
        if (texto.trim().equals(TODOS)) {
            return List.of(TODOS);
        }
        List<String> lista = new ArrayList<>();
        for (String parte : texto.split("[,;\\s]+")) {
            String codigo = parte.trim().toUpperCase(Locale.ROOT);
            if (codigo.isEmpty()) {
                continue;
            }
            if (!CASOS_DE_USO.contains(codigo)) {
                throw new ErrorNegocio("El caso de uso «" + parte.trim() + "» no existe. Use CU1 a CU8 separados por comas, o * para todos.");
            }
            if (!lista.contains(codigo)) {
                lista.add(codigo);
            }
        }
        if (lista.isEmpty()) {
            throw new ErrorNegocio("Indique al menos un caso de uso (por ejemplo CU2,CU4) o * para todos.");
        }
        return lista;
    }

    private Rol rolExistente(Connection con, String nombre) throws SQLException, ErrorNegocio {
        String limpio = normalizarRol(nombre);
        Rol rol = roles.buscarPorNombre(con, limpio);
        if (rol == null) {
            List<String> nombres = new ArrayList<>();
            for (Rol r : roles.listar(con, "*")) {
                nombres.add(r.nombre());
            }
            throw new ErrorNegocio("No existe el rol " + limpio + ". Roles disponibles: " + String.join(", ", nombres) + ".");
        }
        return rol;
    }

    private Rol rolExistente(Connection con, int id) throws SQLException, ErrorNegocio {
        Rol rol = roles.buscar(con, id);
        if (rol == null) {
            throw new ErrorNegocio("No existe un rol con ID " + id + ".");
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
        Rol rol = roles.buscar(con, usuario.rolId());
        boolean esAdministrador = usuario.activo() && rol != null && rol.modulos().contains(TODOS);
        if (esAdministrador && usuarios.contarAdministradoresActivos(con, usuario.id()) == 0
                && Configuracion.lista("admin.correos").isEmpty()) {
            throw new ErrorNegocio(usuario.correo() + " es el único administrador activo: registre otro antes de quitarle el acceso.");
        }
    }
}
