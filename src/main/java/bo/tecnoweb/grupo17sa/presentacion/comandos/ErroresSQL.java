package bo.tecnoweb.grupo17sa.presentacion.comandos;

import java.sql.SQLException;

/** Traduce los errores de PostgreSQL (por su SQLState) a un mensaje claro, sin perder el detalle original. */
final class ErroresSQL {

    private ErroresSQL() {
    }

    static String describir(SQLException e) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        String detalle = " Detalle: " + e.getMessage();
        switch (estado) {
            case "23505":
                return "Ya existe un registro con ese valor (restricción de unicidad)." + detalle;
            case "23503":
                return "El registro está relacionado con otros datos (llave foránea)." + detalle;
            case "23502":
                return "Falta un dato obligatorio." + detalle;
            case "23514":
                return "Un valor no cumple las reglas de la tabla (restricción CHECK)." + detalle;
            case "22001":
                return "Un texto es más largo de lo que admite la columna." + detalle;
            case "28P01":
                return "La base de datos rechazó el usuario o la contraseña de conexión.";
            case "42P01":
                return "Falta una tabla en la base de datos; ejecute el programa con --instalar-bd." + detalle;
            default:
                if (estado.startsWith("08")) {
                    return "No se pudo conectar con la base de datos." + detalle;
                }
                return "SQLState " + estado + "." + detalle;
        }
    }
}
