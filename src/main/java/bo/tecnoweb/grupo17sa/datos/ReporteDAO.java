package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.Conteo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Consultas de los reportes (CU8): conteos agrupados de las tablas de los demás casos de uso.
 * Los nombres de tabla y columna salen de listas fijas del código (nunca del correo), por eso se pueden
 * escribir en el SQL; los valores que vienen del usuario (fechas) van siempre como parámetros.
 */
public class ReporteDAO {

    /** Tablas del resumen general: caso de uso, nombre a mostrar y tabla. Cada integrante agrega las suyas. */
    public static final String[][] TABLAS_RESUMEN = {
            {"CU2", "Banners", "banner"},
            {"CU2", "Contenidos destacados", "destacado"},
            {"CU2", "Accesos rápidos", "acceso_rapido"},
            {"CU2", "Redes sociales", "red_social"},
            {"CU2", "Registros de bitácora", "bitacora"},
            {"CU3", "Información académica", "info_academica"},
            {"CU3", "Materias", "materia"},
            {"CU3", "Plantel docente", "docente"},
            {"CU5", "Convenios", "convenio"},
            {"CU5", "Servicios de INNOVA", "innova_servicio"},
            {"CU5", "Equipo de INNOVA", "innova_miembro"},
            {"CU5", "Proyectos de INNOVA", "innova_proyecto"},
            {"CU5", "Solicitudes a INNOVA", "innova_solicitud"},
            {"CU5", "Graduados", "graduado"},
            {"CU6", "Noticias", "noticia"},
            {"CU6", "Comunicados", "comunicado"},
            {"CU6", "Eventos", "evento"},
            {"CU6", "Galería", "galeria"},
    };

    private static final Set<String> COLUMNAS_BITACORA = Set.of("usuario", "modulo", "accion");
    private static final Set<String> COLUMNAS_CONVENIO = Set.of("tipo", "estado");
    private static final Set<String> COLUMNAS_GRADUADO = Set.of("sector", "ciudad", "anio_titulacion");
    private static final Set<String> COLUMNAS_SOLICITUD = Set.of("estado", "tipo");

    /** Cantidad de filas de una tabla de TABLAS_RESUMEN, o -1 si la tabla todavía no existe (caso de uso no instalado). */
    public long contarFilas(Connection con, String tabla) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
            ps.setString(1, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (!rs.getBoolean(1)) {
                    return -1;
                }
            }
        }
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT count(*) FROM " + tabla)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /** Acciones de la bitácora entre dos fechas (inclusive), agrupadas por usuario, módulo o acción. */
    public List<Conteo> bitacoraPor(Connection con, String columna, LocalDate desde, LocalDate hasta) throws SQLException {
        verificar(columna, COLUMNAS_BITACORA);
        return agrupar(con, "SELECT " + columna + ", count(*) FROM bitacora WHERE fecha >= ? AND fecha < ?"
                + " GROUP BY 1 ORDER BY 2 DESC, 1", desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    public List<Conteo> conveniosPor(Connection con, String columna) throws SQLException {
        verificar(columna, COLUMNAS_CONVENIO);
        return agrupar(con, "SELECT " + columna + ", count(*) FROM convenio GROUP BY 1 ORDER BY 2 DESC, 1");
    }

    public List<Conteo> graduadosPor(Connection con, String columna) throws SQLException {
        verificar(columna, COLUMNAS_GRADUADO);
        String orden = columna.equals("anio_titulacion") ? "1 DESC" : "2 DESC, 1";
        return agrupar(con, "SELECT coalesce(" + columna + "::text, '(sin dato)'), count(*) FROM graduado"
                + " GROUP BY 1 ORDER BY " + orden);
    }

    public List<Conteo> solicitudesPor(Connection con, String columna) throws SQLException {
        verificar(columna, COLUMNAS_SOLICITUD);
        return agrupar(con, "SELECT " + columna + ", count(*) FROM innova_solicitud GROUP BY 1 ORDER BY 2 DESC, 1");
    }

    /** {total, con empleo} de los graduados. */
    public long[] graduadosConEmpleo(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*), count(*) FILTER (WHERE sector <> 'SIN_EMPLEO') FROM graduado")) {
            rs.next();
            return new long[]{rs.getLong(1), rs.getLong(2)};
        }
    }

    /** Días promedio entre que llega una solicitud a INNOVA y se atiende; null si aún no se atendió ninguna. */
    public Double promedioDiasAtencion(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT avg(EXTRACT(EPOCH FROM (atendido_en - fecha)) / 86400.0)"
                     + " FROM innova_solicitud WHERE atendido_en IS NOT NULL")) {
            rs.next();
            double promedio = rs.getDouble(1);
            return rs.wasNull() ? null : promedio;
        }
    }

    private static void verificar(String columna, Set<String> permitidas) {
        if (!permitidas.contains(columna)) {
            throw new IllegalArgumentException("Columna no permitida en el reporte: " + columna);
        }
    }

    private static List<Conteo> agrupar(Connection con, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Conteo> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(new Conteo(rs.getString(1), rs.getLong(2)));
                }
                return lista;
            }
        }
    }
}
