package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.MensajeContactoDAO;
import bo.tecnoweb.grupo17sa.datos.UsuarioDAO;
import bo.tecnoweb.grupo17sa.modelo.MensajeContacto;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * CU7 Formulario de contacto (RF-15). El mensaje queda con el correo del remitente del comando; quienes administran el
 * CU7 reciben un aviso y la respuesta (RESMSJ) le llega al remitente por correo. Contra el spam: como máximo
 * MAXIMO_PENDIENTES mensajes sin responder por correo y un largo mínimo del mensaje.
 */
public class MensajesNegocio {

    public static final String MODULO = "CU7";
    public static final int MAXIMO_PENDIENTES = 3;
    public static final int MINIMO_MENSAJE = 10;

    /** Lo que devuelve ENVMSJ: el número del mensaje y a quiénes avisar. */
    public record Envio(int id, List<String> avisar) {
    }

    private final MensajeContactoDAO mensajes = new MensajeContactoDAO();
    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    public Envio enviar(String correo, String nombre, String telefono, String asunto, String mensaje)
            throws SQLException, ErrorNegocio {
        String remitente = correo.trim().toLowerCase(Locale.ROOT);
        if (mensaje.trim().length() < MINIMO_MENSAJE) {
            throw new ErrorNegocio("El mensaje es muy corto: escriba al menos " + MINIMO_MENSAJE + " caracteres.");
        }
        if (telefono != null && !telefono.matches("[0-9+()\\-\\s]{6,30}")) {
            throw new ErrorNegocio("El teléfono solo admite números, espacios y los signos + ( ) -; o déjelo vacío (\"\").");
        }
        return Transaccion.ejecutar(con -> {
            if (mensajes.contarPendientes(con, remitente) >= MAXIMO_PENDIENTES) {
                throw new ErrorNegocio("Ya tiene " + MAXIMO_PENDIENTES + " mensajes sin responder. Espere la respuesta;"
                        + " puede ver su estado con MISMSJ.");
            }
            int id = mensajes.insertar(con, new MensajeContacto(0, remitente, nombre.trim(), telefono, asunto.trim(),
                    mensaje.trim(), "NUEVO", null, null, null, null));
            bitacora.registrar(con, remitente, BitacoraNegocio.CREAR, MODULO, "mensaje_contacto", id, asunto.trim());
            return new Envio(id, correosQueAtienden(con));
        });
    }

    public List<MensajeContacto> misMensajes(String correo) throws SQLException, ErrorNegocio {
        String remitente = correo.trim().toLowerCase(Locale.ROOT);
        return Transaccion.ejecutar(con -> mensajes.listarPorCorreo(con, remitente));
    }

    public List<MensajeContacto> listar(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> mensajes.listar(con, filtro));
    }

    /** Muestra el mensaje y, si era NUEVO, lo marca como LEIDO. */
    public MensajeContacto ver(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            MensajeContacto m = existente(con, id);
            if (m.estado().equals("NUEVO")) {
                mensajes.cambiarEstado(con, id, "LEIDO");
                bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "mensaje_contacto", id, "Leído: " + m.asunto());
                return mensajes.buscar(con, id);
            }
            return m;
        });
    }

    /** Guarda la respuesta; quien llama la envía por correo al remitente. */
    public MensajeContacto responder(String usuario, int id, String respuesta) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            MensajeContacto m = existente(con, id);
            if (m.estado().equals("RESPONDIDO")) {
                throw new ErrorNegocio("El mensaje " + id + " ya fue respondido por " + m.respondidoPor() + ".");
            }
            if (m.estado().equals("ARCHIVADO")) {
                throw new ErrorNegocio("El mensaje " + id + " está archivado; no se puede responder.");
            }
            mensajes.responder(con, id, respuesta.trim(), usuario);
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "mensaje_contacto", id, "Respondido: " + m.asunto());
            return m;
        });
    }

    public MensajeContacto archivar(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            MensajeContacto m = existente(con, id);
            if (m.estado().equals("ARCHIVADO")) {
                throw new ErrorNegocio("El mensaje " + id + " ya está archivado.");
            }
            mensajes.cambiarEstado(con, id, "ARCHIVADO");
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "mensaje_contacto", id, "Archivado: " + m.asunto());
            return m;
        });
    }

    public MensajeContacto eliminar(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            MensajeContacto m = existente(con, id);
            mensajes.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "mensaje_contacto", id, m.asunto());
            return m;
        });
    }

    private MensajeContacto existente(Connection con, int id) throws SQLException, ErrorNegocio {
        MensajeContacto m = mensajes.buscar(con, id);
        if (m == null) {
            throw new ErrorNegocio("No existe un mensaje con ID " + id + ".");
        }
        return m;
    }

    /** Usuarios activos que atienden el CU7. Si el CU1 no está instalado no hay a quién avisar. */
    private List<String> correosQueAtienden(Connection con) {
        try {
            return usuarios.correosConPermiso(con, MODULO);
        } catch (SQLException e) {
            return List.of();
        }
    }
}
