package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.ConvenioDAO;
import bo.tecnoweb.grupo17sa.datos.RegistroAccesoDAO;
import bo.tecnoweb.grupo17sa.datos.ReporteDAO;
import bo.tecnoweb.grupo17sa.modelo.Conteo;
import bo.tecnoweb.grupo17sa.modelo.Convenio;
import bo.tecnoweb.grupo17sa.modelo.LineaAcceso;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CU8 Reportes y Estadísticas: resúmenes calculados con los datos de los demás casos de uso. */
public class ReporteNegocio {

    /** Una fila del resumen general; cantidad -1 = la tabla aún no está instalada. */
    public record FilaResumen(String modulo, String nombre, long cantidad) {
    }

    public record ActividadBitacora(long total, List<Conteo> porUsuario, List<Conteo> porModulo, List<Conteo> porAccion) {
    }

    public record ReporteVinculacion(List<Conteo> porTipo, List<Conteo> porEstado, List<Convenio> porVencer) {
    }

    public record ReporteAlumni(long total, long conEmpleo, List<Conteo> porSector, List<Conteo> porAnio,
                                List<Conteo> porCiudad) {
    }

    public record ReporteInnova(long total, List<Conteo> porEstado, List<Conteo> porTipo, Double promedioDias) {
    }

    public record UsoSistema(int total, int exitos, double promedioMs, List<Conteo> porComando, List<Conteo> porRemitente) {
    }

    /** Días de anticipación para avisar que un convenio vigente está por vencer. */
    public static final int DIAS_AVISO_VENCIMIENTO = 60;

    private final ReporteDAO reportes = new ReporteDAO();
    private final ConvenioDAO convenios = new ConvenioDAO();
    private final RegistroAccesoDAO accesos = new RegistroAccesoDAO();

    public List<FilaResumen> resumenGeneral() throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            List<FilaResumen> filas = new ArrayList<>();
            for (String[] t : ReporteDAO.TABLAS_RESUMEN) {
                filas.add(new FilaResumen(t[0], t[1], reportes.contarFilas(con, t[2])));
            }
            return filas;
        });
    }

    public ActividadBitacora actividadBitacora(LocalDate desde, LocalDate hasta) throws SQLException, ErrorNegocio {
        validarRango(desde, hasta);
        return Transaccion.ejecutar(con -> {
            List<Conteo> porModulo = reportes.bitacoraPor(con, "modulo", desde, hasta);
            return new ActividadBitacora(sumar(porModulo), reportes.bitacoraPor(con, "usuario", desde, hasta), porModulo,
                    reportes.bitacoraPor(con, "accion", desde, hasta));
        });
    }

    public ReporteVinculacion vinculacion() throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> new ReporteVinculacion(reportes.conveniosPor(con, "tipo"),
                reportes.conveniosPor(con, "estado"), convenios.vigentesPorVencer(con, DIAS_AVISO_VENCIMIENTO)));
    }

    public ReporteAlumni alumni() throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            long[] empleo = reportes.graduadosConEmpleo(con);
            return new ReporteAlumni(empleo[0], empleo[1], reportes.graduadosPor(con, "sector"),
                    reportes.graduadosPor(con, "anio_titulacion"), reportes.graduadosPor(con, "ciudad"));
        });
    }

    public ReporteInnova innova() throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            List<Conteo> porEstado = reportes.solicitudesPor(con, "estado");
            return new ReporteInnova(sumar(porEstado), porEstado, reportes.solicitudesPor(con, "tipo"),
                    reportes.promedioDiasAtencion(con));
        });
    }

    /** Uso del sistema a partir de logs/access.log (no de la base de datos). */
    public UsoSistema usoSistema(LocalDate desde, LocalDate hasta) throws ErrorNegocio {
        validarRango(desde, hasta);
        List<LineaAcceso> lineas;
        try {
            lineas = accesos.leer(desde, hasta);
        } catch (IOException e) {
            throw new ErrorNegocio("No se pudo leer el archivo access.log: " + e.getMessage());
        }
        int exitos = 0;
        long milisegundos = 0;
        Map<String, Long> porComando = new LinkedHashMap<>();
        Map<String, Long> porRemitente = new LinkedHashMap<>();
        for (LineaAcceso l : lineas) {
            if (l.exito()) {
                exitos++;
            }
            milisegundos += l.milisegundos();
            porComando.merge(l.comando(), 1L, Long::sum);
            porRemitente.merge(l.correo(), 1L, Long::sum);
        }
        double promedio = lineas.isEmpty() ? 0 : (double) milisegundos / lineas.size();
        return new UsoSistema(lineas.size(), exitos, promedio, ordenar(porComando), ordenar(porRemitente));
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) throws ErrorNegocio {
        if (hasta.isBefore(desde)) {
            throw new ErrorNegocio("La fecha «hasta» (" + hasta + ") no puede ser anterior a «desde» (" + desde + ").");
        }
    }

    private static long sumar(List<Conteo> conteos) {
        long total = 0;
        for (Conteo c : conteos) {
            total += c.cantidad();
        }
        return total;
    }

    /** De mayor a menor cantidad. */
    private static List<Conteo> ordenar(Map<String, Long> mapa) {
        List<Conteo> lista = new ArrayList<>();
        mapa.forEach((clave, valor) -> lista.add(new Conteo(clave, valor)));
        lista.sort((a, b) -> Long.compare(b.cantidad(), a.cantidad()));
        return lista;
    }
}
