package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.modelo.Conteo;
import bo.tecnoweb.grupo17sa.modelo.Convenio;
import bo.tecnoweb.grupo17sa.negocio.ErrorNegocio;
import bo.tecnoweb.grupo17sa.negocio.ReporteNegocio;
import bo.tecnoweb.grupo17sa.presentacion.lexico.Comando;
import bo.tecnoweb.grupo17sa.presentacion.vista.GraficoBarras;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import bo.tecnoweb.grupo17sa.presentacion.vista.TablaHTML;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.privado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.fecha;

/** CU8 Reportes y Estadísticas. Todos los reportes necesitan permiso sobre el CU8. */
public class ComandosCU8 implements ModuloComandos {

    private static final List<DefinicionComando> COMANDOS = List.of(
            privado("REPGEN", "Resumen general: cantidad de registros de cada caso de uso"),
            privado("REPBIT", "Actividad de la bitácora entre dos fechas, por usuario, módulo y acción",
                    fecha("desde"), fecha("hasta")),
            privado("REPVIN", "Convenios por tipo y estado, y convenios vigentes que vencen en los próximos "
                    + ReporteNegocio.DIAS_AVISO_VENCIMIENTO + " días"),
            privado("REPALU", "Graduados por sector, año de titulación y ciudad, y porcentaje con empleo"),
            privado("REPINN", "Solicitudes a INNOVA por estado y tipo, y tiempo promedio de atención"),
            privado("REPUSO", "Uso del sistema entre dos fechas según access.log: comandos, remitentes y errores",
                    fecha("desde"), fecha("hasta"))
    );

    private final ReporteNegocio reportes = new ReporteNegocio();

    @Override
    public String codigo() {
        return "CU8";
    }

    @Override
    public String nombre() {
        return "Reportes y Estadísticas";
    }

    @Override
    public List<DefinicionComando> comandos() {
        return COMANDOS;
    }

    @Override
    public Respuesta ejecutar(Comando c, String usuario) throws SQLException, ErrorNegocio {
        switch (c.nombre()) {
            case "REPGEN":
                return resumenGeneral();
            case "REPBIT":
                return actividadBitacora(c.fecha(0), c.fecha(1));
            case "REPVIN":
                return vinculacion();
            case "REPALU":
                return alumni();
            case "REPINN":
                return innova();
            case "REPUSO":
                return usoSistema(c.fecha(0), c.fecha(1));
            default:
                throw new IllegalStateException("Comando sin implementar: " + c.nombre());
        }
    }

    private Respuesta resumenGeneral() throws SQLException, ErrorNegocio {
        TablaHTML tabla = new TablaHTML("Registros por caso de uso", "Caso de uso", "Datos", "Cantidad").sinCantidad();
        long total = 0;
        for (ReporteNegocio.FilaResumen f : reportes.resumenGeneral()) {
            tabla.fila(f.modulo(), f.nombre(), f.cantidad() < 0 ? "(no instalado)" : f.cantidad());
            total += Math.max(0, f.cantidad());
        }
        return Respuesta.exito("Resumen general", "Reporte generado: " + total + " registros en total.").agregar(tabla);
    }

    private Respuesta actividadBitacora(LocalDate desde, LocalDate hasta) throws SQLException, ErrorNegocio {
        ReporteNegocio.ActividadBitacora a = reportes.actividadBitacora(desde, hasta);
        return Respuesta.exito("Actividad de la bitácora", "Reporte generado: " + a.total() + " acciones del "
                        + Respuestas.fecha(desde) + " al " + Respuestas.fecha(hasta) + ".")
                .agregar(grafico("Por usuario", a.porUsuario()))
                .agregar(grafico("Por caso de uso", a.porModulo()))
                .agregar(grafico("Por acción", a.porAccion()));
    }

    private Respuesta vinculacion() throws SQLException, ErrorNegocio {
        ReporteNegocio.ReporteVinculacion v = reportes.vinculacion();
        TablaHTML vencen = new TablaHTML("Vigentes que vencen en " + ReporteNegocio.DIAS_AVISO_VENCIMIENTO
                + " días o ya vencieron", "ID", "Institución", "Tipo", "Fecha de fin", "Días restantes");
        for (Convenio cv : v.porVencer()) {
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), cv.fechaFin());
            vencen.fila(cv.id(), cv.institucion(), cv.tipo(), Respuestas.fecha(cv.fechaFin()),
                    dias < 0 ? "vencido hace " + (-dias) : dias);
        }
        return Respuesta.exito("Reporte de vinculación", "Reporte generado: " + sumar(v.porTipo()) + " convenios registrados.")
                .agregar(grafico("Por tipo", v.porTipo()))
                .agregar(grafico("Por estado", v.porEstado()))
                .agregar(vencen);
    }

    private Respuesta alumni() throws SQLException, ErrorNegocio {
        ReporteNegocio.ReporteAlumni a = reportes.alumni();
        String empleo = a.total() == 0 ? "0%" : String.format(Locale.ROOT, "%.0f%%", a.conEmpleo() * 100.0 / a.total());
        return Respuesta.exito("Reporte de graduados", "Reporte generado: " + a.total() + " graduados registrados; "
                        + a.conEmpleo() + " con empleo (" + empleo + ").")
                .agregar(grafico("Por sector", a.porSector()))
                .agregar(grafico("Por año de titulación", a.porAnio()))
                .agregar(grafico("Por ciudad", a.porCiudad()));
    }

    private Respuesta innova() throws SQLException, ErrorNegocio {
        ReporteNegocio.ReporteInnova i = reportes.innova();
        String promedio = i.promedioDias() == null ? "todavía no se atendió ninguna"
                : i.promedioDias() * 24 < 1 ? "menos de una hora en promedio"
                : i.promedioDias() < 1 ? String.format(Locale.ROOT, "%.1f horas en promedio", i.promedioDias() * 24)
                : String.format(Locale.ROOT, "%.1f días en promedio", i.promedioDias());
        return Respuesta.exito("Reporte de INNOVA", "Reporte generado: " + i.total() + " solicitudes; tiempo de atención: "
                        + promedio + ".")
                .agregar(grafico("Por estado", i.porEstado()))
                .agregar(grafico("Por tipo", i.porTipo()));
    }

    private Respuesta usoSistema(LocalDate desde, LocalDate hasta) throws ErrorNegocio {
        ReporteNegocio.UsoSistema u = reportes.usoSistema(desde, hasta);
        TablaHTML resumen = new TablaHTML("Resumen", "Dato", "Valor").sinCantidad()
                .fila("Correos procesados", u.total())
                .fila("Con éxito", u.exitos())
                .fila("Con error", u.total() - u.exitos())
                .fila("Tiempo promedio de respuesta", String.format(Locale.ROOT, "%.0f ms", u.promedioMs()));
        return Respuesta.exito("Uso del sistema", "Reporte generado con access.log del " + Respuestas.fecha(desde)
                        + " al " + Respuestas.fecha(hasta) + ".")
                .agregar(resumen)
                .agregar(grafico("Comandos más usados", u.porComando()))
                .agregar(grafico("Por remitente", u.porRemitente()));
    }

    private static GraficoBarras grafico(String titulo, List<Conteo> conteos) {
        GraficoBarras g = new GraficoBarras(titulo);
        for (Conteo c : conteos) {
            g.agregar(c.etiqueta(), c.cantidad());
        }
        return g;
    }

    private static long sumar(List<Conteo> conteos) {
        long total = 0;
        for (Conteo c : conteos) {
            total += c.cantidad();
        }
        return total;
    }
}
