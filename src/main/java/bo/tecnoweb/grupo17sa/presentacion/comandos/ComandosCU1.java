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

import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.privado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.publico;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.booleano;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.correo;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.entero;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.filtro;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.texto;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.textoOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.eliminado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.fecha;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.insertado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.listado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.modificado;

/** CU1 Gestión de Usuarios, roles y permisos (RF-18). */
public class ComandosCU1 implements ModuloComandos {

    private static final String AYUDA_CASOS = "casos_de_uso: CU1 a CU8 separados por comas (CU2,CU4) o * para todos";

    private static final List<DefinicionComando> COMANDOS = List.of(
            privado("LISUSU", "Lista los usuarios: \"*\" (todos) o un correo, nombre o rol", filtro()),
            privado("INSUSU", "Registra un usuario; el rol es uno de LISROL", correo("correo", 120), texto("nombre", 150),
                    texto("rol", 40)),
            privado("MODUSU", "Modifica el nombre, el rol y el estado de un usuario (activo: SI o NO)",
                    entero("id"), texto("nombre", 150), texto("rol", 40), booleano("activo")),
            privado("ELIUSU", "Elimina un usuario", entero("id")),
            privado("LISROL", "Lista los roles con sus casos de uso y su cantidad de usuarios", filtro()),
            privado("INSROL", "Registra un rol. " + AYUDA_CASOS, texto("nombre", 40), textoOpcional("descripcion", 200),
                    texto("casos_de_uso", 60)),
            privado("MODROL", "Modifica un rol. " + AYUDA_CASOS, entero("id"), texto("nombre", 40),
                    textoOpcional("descripcion", 200), texto("casos_de_uso", 60)),
            privado("ELIROL", "Elimina un rol que no tenga usuarios", entero("id")),
            publico("MISROL", "Muestra su usuario, su rol y los casos de uso que puede administrar")
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
                        .agregar(new Parrafo("Acceso", "Desde ahora " + c.parametro(0).trim().toLowerCase()
                                + " puede usar los comandos de su rol. Con MISROL ve lo que puede administrar."));
            case "MODUSU":
                usuarios.modificarUsuario(usuario, c.entero(0), c.parametro(1), c.parametro(2), c.booleano(3));
                return modificado("usuario", c.entero(0));
            case "ELIUSU":
                return eliminado("usuario", c.entero(0), usuarios.eliminarUsuario(usuario, c.entero(0)).correo());
            case "LISROL":
                return listado("Roles", tablaRoles(usuarios.listarRoles(c.parametro(0))));
            case "INSROL":
                return insertado("rol", usuarios.insertarRol(usuario, c.parametro(0), c.texto(1), c.parametro(2)));
            case "MODROL":
                usuarios.modificarRol(usuario, c.entero(0), c.parametro(1), c.texto(2), c.parametro(3));
                return modificado("rol", c.entero(0));
            case "ELIROL":
                return eliminado("rol", c.entero(0), usuarios.eliminarRol(usuario, c.entero(0)).nombre());
            case "MISROL":
                return miCuenta(usuarios.miCuenta(usuario));
            default:
                throw new IllegalStateException("Comando sin implementar: " + c.nombre());
        }
    }

    private static Respuesta miCuenta(UsuariosNegocio.MiCuenta cuenta) {
        if (cuenta.usuario() == null) {
            String mensaje = cuenta.administradorArranque()
                    ? "Su correo es administrador de arranque (config.properties): puede usar todos los comandos."
                    + " Registre los usuarios con INSUSU."
                    : "El correo " + cuenta.correo() + " no está registrado como usuario: puede usar los comandos públicos."
                    + " Envíe HELP para verlos.";
            return Respuesta.exito("Su cuenta", mensaje);
        }
        Usuario u = cuenta.usuario();
        Rol rol = cuenta.rol();
        String casos = rol == null || rol.modulos().isEmpty() ? "ninguno"
                : rol.modulos().contains(UsuariosNegocio.TODOS) ? "todos" : String.join(", ", rol.modulos());
        TablaHTML t = new TablaHTML("Su usuario", "Dato", "Valor").sinCantidad()
                .fila("Nombre", u.nombre())
                .fila("Correo", u.correo())
                .fila("Rol", u.rol())
                .fila("Casos de uso que administra", casos)
                .fila("Estado", u.activo() ? "Activo" : "Inactivo (solo comandos públicos)");
        return Respuesta.exito("Su cuenta", "Datos de su usuario en el sistema.").agregar(t);
    }

    private static TablaHTML tablaUsuarios(List<Usuario> lista) {
        TablaHTML t = new TablaHTML("Usuarios", "ID", "Correo", "Nombre", "Rol", "Activo", "Registrado");
        for (Usuario u : lista) {
            t.fila(u.id(), u.correo(), u.nombre(), u.rol(), u.activo() ? "Sí" : "No", fecha(u.creado()));
        }
        return t;
    }

    private static TablaHTML tablaRoles(List<Rol> lista) {
        TablaHTML t = new TablaHTML("Roles", "ID", "Rol", "Descripción", "Casos de uso", "Usuarios");
        for (Rol r : lista) {
            String casos = r.modulos().contains(UsuariosNegocio.TODOS) ? "Todos (*)" : String.join(", ", r.modulos());
            t.fila(r.id(), r.nombre(), r.descripcion(), casos, r.usuarios());
        }
        return t;
    }
}
