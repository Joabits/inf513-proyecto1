package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.modelo.Rol;
import bo.tecnoweb.grupo17sa.modelo.Usuario;
import bo.tecnoweb.grupo17sa.negocio.ErrorNegocio;
import bo.tecnoweb.grupo17sa.negocio.UsuariosNegocio;
import bo.tecnoweb.grupo17sa.presentacion.lexico.Comando;
import bo.tecnoweb.grupo17sa.presentacion.vista.Parrafo;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import bo.tecnoweb.grupo17sa.presentacion.vista.TablaHTML;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.privado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.publico;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.booleano;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.correo;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.entero;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.filtro;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.texto;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.eliminado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.fecha;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.insertado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.listado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.modificado;

/** CU1 Gestión de Usuarios (RF-18). Los roles son fijos: aquí solo se consultan, no se crean ni se modifican. */
public class ComandosCU1 implements ModuloComandos {

    private static final String ROLES = "rol: ADMINISTRADOR, EDITOR_INSTITUCIONAL o EDITOR_COMUNICACION";

    /** Lo que se muestra del Visitante: no es un rol de la base, es cualquier correo sin usuario activo. */
    private static final String VISITANTE = "Cualquier correo sin usuario activo, sin registrarse: solo los comandos"
            + " públicos (portal, consultas, contacto, búsqueda, solicitudes a INNOVA y Alumni).";

    private static final List<DefinicionComando> COMANDOS = List.of(
            privado("LISUSU", "Lista los usuarios: \"*\" (todos) o un correo, nombre o rol", filtro()),
            privado("INSUSU", "Registra un usuario. " + ROLES, correo("correo", 120), texto("nombre", 150),
                    texto("rol", 40)),
            privado("MODUSU", "Modifica el nombre, el rol y el estado de un usuario (activo: SI o NO). " + ROLES,
                    entero("id"), texto("nombre", 150), texto("rol", 40), booleano("activo")),
            privado("ELIUSU", "Elimina un usuario", entero("id")),
            privado("LISROL", "Lista los roles fijos del sistema, lo que gestiona cada uno y su cantidad de usuarios",
                    filtro()),
            publico("MISROL", "Muestra su usuario, su rol y lo que puede gestionar")
    );

    private final UsuariosNegocio usuarios = new UsuariosNegocio();

    @Override
    public String codigo() {
        return UsuariosNegocio.MODULO;
    }

    @Override
    public String nombre() {
        return "Gestión de Usuarios";
    }

    @Override
    public List<DefinicionComando> comandos() {
        return COMANDOS;
    }

    @Override
    public Respuesta ejecutar(Comando c, String usuario) throws SQLException, ErrorNegocio {
        switch (c.nombre()) {
            case "LISUSU":
                return listado("Usuarios", tablaUsuarios(usuarios.listarUsuarios(c.parametro(0))));
            case "INSUSU":
                return insertado("usuario", usuarios.insertarUsuario(usuario, c.parametro(0), c.parametro(1), c.parametro(2)))
                        .agregar(new Parrafo("Acceso", "Desde ahora " + c.parametro(0).trim().toLowerCase(Locale.ROOT)
                                + " puede usar los comandos de su rol. Con MISROL ve lo que puede gestionar."));
            case "MODUSU":
                usuarios.modificarUsuario(usuario, c.entero(0), c.parametro(1), c.parametro(2), c.booleano(3));
                return modificado("usuario", c.entero(0));
            case "ELIUSU":
                return eliminado("usuario", c.entero(0), usuarios.eliminarUsuario(usuario, c.entero(0)).correo());
            case "LISROL":
                return listado("Roles", tablaRoles(usuarios.listarRoles(), c.parametro(0)));
            case "MISROL":
                return miCuenta(usuarios.miCuenta(usuario));
            default:
                throw new IllegalStateException("Comando sin implementar: " + c.nombre());
        }
    }

    private static Respuesta miCuenta(UsuariosNegocio.MiCuenta cuenta) {
        Usuario u = cuenta.usuario();
        if (u == null) {
            String mensaje = cuenta.administradorArranque()
                    ? "Su correo es administrador de arranque (config.properties): tiene los permisos del rol"
                    + " Administrador. Registre los usuarios con INSUSU."
                    : "El correo " + cuenta.correo() + " no está registrado como usuario: es Visitante y puede usar"
                    + " los comandos públicos. Envíe HELP para verlos.";
            return Respuesta.exito("Su cuenta", mensaje);
        }
        TablaHTML t = new TablaHTML("Su usuario", "Dato", "Valor").sinCantidad()
                .fila("Nombre", u.nombre())
                .fila("Correo", u.correo())
                .fila("Rol", u.rol().nombre() + " (" + u.rol().name() + ")")
                .fila("Qué gestiona", u.rol().gestiona())
                .fila("Estado", u.activo() ? "Activo" : "Inactivo: mientras tanto es Visitante (solo comandos públicos)");
        return Respuesta.exito("Su cuenta", "Datos de su usuario en el sistema.").agregar(t);
    }

    private static TablaHTML tablaUsuarios(List<Usuario> lista) {
        TablaHTML t = new TablaHTML("Usuarios", "ID", "Correo", "Nombre", "Rol", "Activo", "Registrado");
        for (Usuario u : lista) {
            t.fila(u.id(), u.correo(), u.nombre(), u.rol().name(), u.activo() ? "Sí" : "No", fecha(u.creado()));
        }
        return t;
    }

    /** Los tres roles fijos y, al final, el Visitante. El filtro busca en el rol, su nombre y lo que gestiona. */
    private static TablaHTML tablaRoles(Map<Rol, Integer> roles, String filtro) {
        TablaHTML t = new TablaHTML("Roles", "Rol", "Nombre", "Qué gestiona", "Usuarios");
        for (Map.Entry<Rol, Integer> e : roles.entrySet()) {
            Rol r = e.getKey();
            if (coincide(filtro, r.name(), r.nombre(), r.gestiona())) {
                t.fila(r.name(), r.nombre(), r.gestiona(), e.getValue());
            }
        }
        if (coincide(filtro, "VISITANTE", "Visitante", VISITANTE)) {
            t.fila("VISITANTE", "Visitante", VISITANTE, "-");
        }
        return t;
    }

    private static boolean coincide(String filtro, String... textos) {
        if (filtro == null || filtro.isBlank() || filtro.trim().equals("*")) {
            return true;
        }
        String buscado = filtro.trim().toLowerCase(Locale.ROOT);
        for (String texto : textos) {
            if (texto.toLowerCase(Locale.ROOT).contains(buscado)) {
                return true;
            }
        }
        return false;
    }
}
