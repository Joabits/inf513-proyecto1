package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.BitacoraDAO;
import bo.tecnoweb.grupo17sa.modelo.RegistroBitacora;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Bitácora de auditoría (RF-19). Todos los casos de uso llaman a registrar() dentro de su transacción,
 * después de crear, modificar o eliminar algo, así la acción y su registro se guardan juntos.
 */
public class BitacoraNegocio {

    public static final String CREAR = "CREAR";
    public static final String MODIFICAR = "MODIFICAR";
    public static final String ELIMINAR = "ELIMINAR";
    public static final String PUBLICAR = "PUBLICAR";

    private final BitacoraDAO dao = new BitacoraDAO();

    /**
     * @param usuario  correo de quien hizo la acción
     * @param accion   CREAR, MODIFICAR, ELIMINAR o PUBLICAR
     * @param modulo   caso de uso, por ejemplo "CU2"
     * @param tabla    tabla afectada
     * @param elemento id o clave del registro afectado
     * @param detalle  texto que identifica el registro (por ejemplo, su título)
     */
    public void registrar(Connection con, String usuario, String accion, String modulo, String tabla,
                          Object elemento, String detalle) throws SQLException {
        dao.registrar(con, usuario, accion, modulo, tabla, elemento == null ? null : String.valueOf(elemento), detalle);
    }

    /** "*" todo; una fecha (AAAA-MM-DD o DD/MM/AAAA) los registros de ese día; otro texto busca coincidencias. */
    public List<RegistroBitacora> listar(String filtro) throws SQLException, ErrorNegocio {
        LocalDate dia = leerFecha(filtro);
        return Transaccion.ejecutar(con -> dia != null ? dao.listarDia(con, dia) : dao.listar(con, filtro));
    }

    private static LocalDate leerFecha(String texto) {
        try {
            if (texto.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(texto);
            }
            if (texto.matches("\\d{2}/\\d{2}/\\d{4}")) {
                return LocalDate.parse(texto, DateTimeFormatter.ofPattern("dd/MM/uuuu"));
            }
        } catch (DateTimeParseException e) {
            return null;
        }
        return null;
    }
}
