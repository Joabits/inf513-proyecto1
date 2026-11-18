package bo.tecnoweb.grupo17sa.datos;

import bo.tecnoweb.grupo17sa.modelo.ResultadoBusqueda;
import bo.tecnoweb.grupo17sa.modelo.ResultadoBusqueda.Coincidencia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Búsqueda global en el contenido publicado de todos los casos de uso (RF-17). Cada palabra debe aparecer en alguna
 * columna de la fuente; no distingue mayúsculas ni tildes. Una fuente cuya tabla todavía no existe (caso de uso no
 * instalado) se salta. Para sumar un caso de uso nuevo se agrega su fuente en FUENTES.
 */
public class BusquedaDAO {

    /** Las mismas letras que se cambian en la columna y en la palabra buscada. */
    public static final String CON_TILDE = "áéíóúüàèìòù";
    public static final String SIN_TILDE = "aeiouuaeiou";

    private static final int POR_SECCION = 5;

    /**
     * Qué se busca en cada tabla. Todo lo de aquí lo escribe el programa; del correo solo llegan valores enlazados (?).
     *
     * @param tabla       tabla principal (para saber si existe)
     * @param desde       tabla con sus uniones
     * @param titulo      expresión del título que se muestra
     * @param detalle     expresión del dato secundario
     * @param columnas    columnas donde se busca
     * @param publicado   condición de lo visible al público, o null si todo es público
     */
    private record Fuente(String modulo, String seccion, String tabla, String desde, String titulo, String detalle,
                          List<String> columnas, String publicado, String orden) {
    }

    private static final List<Fuente> FUENTES = List.of(
            new Fuente("CU2", "Información institucional", "seccion_institucional", "seccion_institucional",
                    "titulo", "''", List.of("titulo", "contenido"), null, "orden"),
            new Fuente("CU2", "Contenidos destacados", "destacado", "destacado",
                    "titulo", "COALESCE(descripcion, '')", List.of("titulo", "descripcion"), null, "orden"),
            new Fuente("CU2", "Banners de inicio", "banner", "banner",
                    "titulo", "COALESCE(subtitulo, '')", List.of("titulo", "subtitulo"), null, "orden"),
            new Fuente("CU2", "Accesos rápidos", "acceso_rapido", "acceso_rapido",
                    "nombre", "url", List.of("nombre", "descripcion"), null, "orden"),
            new Fuente("CU4", "Líneas de investigación", "investigacion_linea", "investigacion_linea",
                    "nombre", "''", List.of("nombre", "descripcion"), null, "nombre"),
            new Fuente("CU4", "Proyectos de investigación", "investigacion_proyecto",
                    "investigacion_proyecto p LEFT JOIN investigacion_linea l ON l.id = p.linea_id",
                    "p.titulo", "p.responsable || ' · ' || p.gestion",
                    List.of("p.titulo", "p.resumen", "p.responsable", "p.integrantes", "l.nombre"), "p.publicado",
                    "p.gestion DESC, p.titulo"),
            new Fuente("CU4", "Documentos", "documento", "documento",
                    "titulo", "categoria", List.of("titulo", "descripcion", "categoria"), "publicado", "titulo"),
            new Fuente("CU4", "Revista INNOVAR: ediciones", "revista_edicion", "revista_edicion",
                    "COALESCE(titulo, 'Número ' || numero)", "'Número ' || numero || ' · ' || anio",
                    List.of("titulo", "descripcion"), "publicada", "anio DESC, numero DESC"),
            new Fuente("CU4", "Revista INNOVAR: artículos", "revista_articulo",
                    "revista_articulo a JOIN revista_edicion e ON e.id = a.edicion_id",
                    "a.titulo", "a.autores || ' · Número ' || e.numero || ' · ' || e.anio",
                    List.of("a.titulo", "a.autores", "a.resumen"), "e.publicada", "e.anio DESC, a.orden"),
            new Fuente("CU5", "Convenios y vinculación", "convenio", "convenio",
                    "institucion", "tipo || ' · ' || estado", List.of("institucion", "objeto", "tipo"), null, "fecha_inicio DESC"),
            new Fuente("CU5", "Servicios de INNOVA", "innova_servicio", "innova_servicio",
                    "nombre", "''", List.of("nombre", "descripcion"), null, "nombre"),
            new Fuente("CU5", "Proyectos de INNOVA", "innova_proyecto", "innova_proyecto",
                    "nombre", "COALESCE(cliente, '')", List.of("nombre", "cliente", "descripcion"), null, "anio DESC")
    );

    /** Secciones con al menos una coincidencia, en el orden de FUENTES. */
    public List<ResultadoBusqueda> buscar(Connection con, List<String> palabras) throws SQLException {
        List<ResultadoBusqueda> resultados = new ArrayList<>();
        for (Fuente f : FUENTES) {
            if (!existe(con, f.tabla())) {
                continue;
            }
            String donde = condicion(f, palabras);
            int total;
            try (PreparedStatement ps = con.prepareStatement("SELECT count(*) FROM " + f.desde() + " WHERE " + donde)) {
                enlazar(ps, f, palabras);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            if (total == 0) {
                continue;
            }
            String sql = "SELECT " + f.titulo() + " AS titulo, " + f.detalle() + " AS detalle FROM " + f.desde()
                    + " WHERE " + donde + " ORDER BY " + f.orden() + " LIMIT " + POR_SECCION;
            List<Coincidencia> coincidencias = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                enlazar(ps, f, palabras);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        coincidencias.add(new Coincidencia(rs.getString("titulo"), rs.getString("detalle")));
                    }
                }
            }
            resultados.add(new ResultadoBusqueda(f.modulo(), f.seccion(), total, coincidencias));
        }
        return resultados;
    }

    private static boolean existe(Connection con, String tabla) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
            ps.setString(1, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    /** (palabra 1 en alguna columna) AND (palabra 2 en alguna columna) ... AND lo publicado. */
    private static String condicion(Fuente f, List<String> palabras) {
        StringBuilder sb = new StringBuilder(f.publicado() == null ? "TRUE" : f.publicado());
        for (int i = 0; i < palabras.size(); i++) {
            sb.append(" AND (");
            for (int j = 0; j < f.columnas().size(); j++) {
                sb.append(j == 0 ? "" : " OR ").append("translate(lower(COALESCE(").append(f.columnas().get(j))
                        .append("::text, '')), '").append(CON_TILDE).append("', '").append(SIN_TILDE).append("') LIKE ? ESCAPE '!'");
            }
            sb.append(")");
        }
        return sb.toString();
    }

    private static void enlazar(PreparedStatement ps, Fuente f, List<String> palabras) throws SQLException {
        int i = 1;
        for (String palabra : palabras) {
            String patron = "%" + palabra.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            for (int j = 0; j < f.columnas().size(); j++) {
                ps.setString(i++, patron);
            }
        }
    }
}
