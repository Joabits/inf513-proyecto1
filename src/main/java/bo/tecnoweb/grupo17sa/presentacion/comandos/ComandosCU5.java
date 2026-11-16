package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.modelo.Convenio;
import bo.tecnoweb.grupo17sa.modelo.Graduado;
import bo.tecnoweb.grupo17sa.modelo.MiembroInnova;
import bo.tecnoweb.grupo17sa.modelo.ProyectoInnova;
import bo.tecnoweb.grupo17sa.modelo.ServicioInnova;
import bo.tecnoweb.grupo17sa.modelo.SolicitudInnova;
import bo.tecnoweb.grupo17sa.negocio.AlumniNegocio;
import bo.tecnoweb.grupo17sa.negocio.ErrorNegocio;
import bo.tecnoweb.grupo17sa.negocio.InnovaNegocio;
import bo.tecnoweb.grupo17sa.negocio.VinculacionNegocio;
import bo.tecnoweb.grupo17sa.presentacion.lexico.Comando;
import bo.tecnoweb.grupo17sa.presentacion.vista.ArchivoCSV;
import bo.tecnoweb.grupo17sa.presentacion.vista.Parrafo;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import bo.tecnoweb.grupo17sa.presentacion.vista.TablaHTML;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.privado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.publico;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.correoOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.entero;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.fecha;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.fechaOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.filtro;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.opcion;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.texto;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.textoOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.urlOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.eliminado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.insertado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.listado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.modificado;

/** CU5 Vinculación, Empresa Junior INNOVA y Alumni (RF-09, RF-10 y RF-11). */
public class ComandosCU5 implements ModuloComandos {

    // Parámetros que se repiten en INS y MOD (los mismos valores del CHECK de cada tabla)
    private static final Parametro[] CONVENIO = {
            opcion("tipo", "CONVENIO", "PRACTICA", "PROYECTO", "CONSULTORIA", "INTERACCION"),
            texto("institucion", 200), texto("objeto", 500), fecha("fecha_inicio"), fechaOpcional("fecha_fin"),
            opcion("estado", "VIGENTE", "EN_TRAMITE", "CONCLUIDO"), textoOpcional("responsable", 150),
            urlOpcional("logo_url", 500)
    };
    private static final Parametro[] SERVICIO = {texto("nombre", 150), texto("descripcion", 500)};
    private static final Parametro[] MIEMBRO = {
            texto("nombre", 150), texto("cargo", 100), textoOpcional("semestre", 30), correoOpcional("correo", 120)
    };
    private static final Parametro[] PROYECTO = {
            texto("nombre", 200), textoOpcional("cliente", 200), texto("descripcion", 500), entero("anio"),
            textoOpcional("resultado", 500)
    };
    private static final Parametro[] GRADUADO = {
            texto("nombre", 100), texto("apellidos", 100), textoOpcional("telefono", 30), entero("anio_titulacion"),
            textoOpcional("empleo_actual", 200), textoOpcional("cargo", 120),
            opcion("sector", "PUBLICO", "PRIVADO", "INDEPENDIENTE", "ONG", "SIN_EMPLEO"), textoOpcional("ciudad", 80)
    };

    private static final List<DefinicionComando> COMANDOS = List.of(
            // RF-09 Convenios y vinculación
            publico("LISCNV", "Lista convenios, prácticas, proyectos con empresas, consultorías y actividades de "
                    + "interacción social (filtre por tipo, institución o estado)", filtro()),
            privado("INSCNV", "Registra un convenio o actividad de vinculación", CONVENIO),
            privado("MODCNV", "Modifica un convenio", conId(CONVENIO)),
            privado("ELICNV", "Elimina un convenio", entero("id")),

            // RF-10 Empresa Junior INNOVA
            publico("INNOVA", "Empresa Junior INNOVA: descripción, servicios, equipo y proyectos realizados"),
            privado("MODINN", "Modifica la descripción de INNOVA", texto("descripcion", 2000)),
            privado("LISSRV", "Lista los servicios de INNOVA", filtro()),
            privado("INSSRV", "Registra un servicio de INNOVA", SERVICIO),
            privado("MODSRV", "Modifica un servicio de INNOVA", conId(SERVICIO)),
            privado("ELISRV", "Elimina un servicio de INNOVA", entero("id")),
            privado("LISMIE", "Lista el equipo de INNOVA", filtro()),
            privado("INSMIE", "Registra un miembro del equipo de INNOVA", MIEMBRO),
            privado("MODMIE", "Modifica un miembro del equipo de INNOVA", conId(MIEMBRO)),
            privado("ELIMIE", "Elimina un miembro del equipo de INNOVA", entero("id")),
            privado("LISPRY", "Lista los proyectos realizados por INNOVA", filtro()),
            privado("INSPRY", "Registra un proyecto realizado por INNOVA", PROYECTO),
            privado("MODPRY", "Modifica un proyecto de INNOVA", conId(PROYECTO)),
            privado("ELIPRY", "Elimina un proyecto de INNOVA", entero("id")),
            publico("SOLINN", "Una empresa solicita un servicio (SERVICIO) o un estudiante postula (POSTULACION) a INNOVA;"
                    + " la solicitud queda con el correo del remitente",
                    opcion("tipo", "SERVICIO", "POSTULACION"), texto("nombre", 150), textoOpcional("telefono", 30),
                    textoOpcional("organizacion", 200), texto("mensaje", 1000)),
            publico("MISSOL", "Muestra el estado de sus solicitudes a INNOVA"),
            privado("LISSOL", "Lista las solicitudes a INNOVA (\"PENDIENTE\" muestra las que faltan atender)", filtro()),
            privado("ATESOL", "Marca una solicitud como ATENDIDA o RECHAZADA y avisa al solicitante por correo",
                    entero("id"), opcion("estado", "ATENDIDA", "RECHAZADA"), texto("observacion", 1000)),

            // RF-11 Alumni
            publico("REGGRA", "Un graduado se registra en Alumni con su correo", GRADUADO),
            publico("ACTGRA", "Un graduado actualiza sus datos", GRADUADO),
            publico("MISGRA", "Muestra sus datos de graduado"),
            privado("LISGRA", "Consulta los graduados (filtre por año, sector, ciudad, empresa...)", filtro()),
            privado("EXPGRA", "Exporta los graduados a un archivo CSV adjunto que se abre en Excel", filtro()),
            privado("ELIGRA", "Elimina el registro de un graduado", entero("id"))
    );

    private final VinculacionNegocio vinculacion = new VinculacionNegocio();
    private final InnovaNegocio innova = new InnovaNegocio();
    private final AlumniNegocio alumni = new AlumniNegocio();

    @Override
    public String codigo() {
        return "CU5";
    }

    @Override
    public String nombre() {
        return "Vinculación, Empresa Junior INNOVA y Alumni";
    }

    @Override
    public List<DefinicionComando> comandos() {
        return COMANDOS;
    }

    @Override
    public Respuesta ejecutar(Comando c, String usuario) throws SQLException, ErrorNegocio {
        switch (c.nombre()) {
            // ---------------------------------------------------- RF-09
            case "LISCNV":
                return listado("Convenios y vinculación", tablaConvenios(vinculacion.listar(c.parametro(0))));
            case "INSCNV":
                return insertado("convenio", vinculacion.insertar(usuario, convenio(0, c, 0)));
            case "MODCNV":
                vinculacion.modificar(usuario, convenio(c.entero(0), c, 1));
                return modificado("convenio", c.entero(0));
            case "ELICNV":
                return eliminado("convenio", c.entero(0), vinculacion.eliminar(usuario, c.entero(0)).institucion());

            // ---------------------------------------------------- RF-10
            case "INNOVA":
                return paginaInnova();
            case "MODINN":
                innova.modificarDescripcion(usuario, c.parametro(0));
                return modificado("descripción de INNOVA", "innova_descripcion");
            case "LISSRV":
                return listado("Servicios de INNOVA", tablaServicios(innova.listarServicios(c.parametro(0)), true));
            case "INSSRV":
                return insertado("servicio de INNOVA", innova.insertarServicio(usuario, servicio(0, c, 0)));
            case "MODSRV":
                innova.modificarServicio(usuario, servicio(c.entero(0), c, 1));
                return modificado("servicio de INNOVA", c.entero(0));
            case "ELISRV":
                return eliminado("servicio de INNOVA", c.entero(0), innova.eliminarServicio(usuario, c.entero(0)).nombre());
            case "LISMIE":
                return listado("Equipo de INNOVA", tablaMiembros(innova.listarMiembros(c.parametro(0)), true));
            case "INSMIE":
                return insertado("miembro de INNOVA", innova.insertarMiembro(usuario, miembro(0, c, 0)));
            case "MODMIE":
                innova.modificarMiembro(usuario, miembro(c.entero(0), c, 1));
                return modificado("miembro de INNOVA", c.entero(0));
            case "ELIMIE":
                return eliminado("miembro de INNOVA", c.entero(0), innova.eliminarMiembro(usuario, c.entero(0)).nombre());
            case "LISPRY":
                return listado("Proyectos de INNOVA", tablaProyectos(innova.listarProyectos(c.parametro(0)), true));
            case "INSPRY":
                return insertado("proyecto de INNOVA", innova.insertarProyecto(usuario, proyecto(0, c, 0)));
            case "MODPRY":
                innova.modificarProyecto(usuario, proyecto(c.entero(0), c, 1));
                return modificado("proyecto de INNOVA", c.entero(0));
            case "ELIPRY":
                return eliminado("proyecto de INNOVA", c.entero(0), innova.eliminarProyecto(usuario, c.entero(0)).nombre());
            case "SOLINN":
                return solicitar(c, usuario);
            case "MISSOL":
                return listado("Mis solicitudes a INNOVA", tablaSolicitudes(innova.misSolicitudes(usuario), false));
            case "LISSOL":
                return listado("Solicitudes a INNOVA", tablaSolicitudes(innova.listarSolicitudes(c.parametro(0)), true));
            case "ATESOL":
                return atender(c, usuario);

            // ---------------------------------------------------- RF-11
            case "REGGRA":
                return insertado("graduado", alumni.registrar(graduado(usuario, c)))
                        .agregar(new Parrafo("Siguiente paso", "Puede ver sus datos con MISGRA y actualizarlos con ACTGRA."));
            case "ACTGRA":
                alumni.actualizar(graduado(usuario, c));
                return modificado("graduado", usuario);
            case "MISGRA":
                return fichaGraduado(alumni.miRegistro(usuario));
            case "LISGRA":
                return listado("Graduados (Alumni)", tablaGraduados(alumni.listar(c.parametro(0))));
            case "EXPGRA":
                return exportarGraduados(alumni.listar(c.parametro(0)));
            case "ELIGRA":
                Graduado borrado = alumni.eliminar(usuario, c.entero(0));
                return eliminado("graduado", c.entero(0), borrado.nombre() + " " + borrado.apellidos());
            default:
                throw new IllegalStateException("Comando sin implementar: " + c.nombre());
        }
    }

    // ================================================================ Armado de objetos desde los parámetros

    private static Parametro[] conId(Parametro... resto) {
        Parametro[] todos = new Parametro[resto.length + 1];
        todos[0] = entero("id");
        System.arraycopy(resto, 0, todos, 1, resto.length);
        return todos;
    }

    /** Valor de una opción en mayúsculas y sin tildes ("práctica" → "PRACTICA"). */
    private static String op(Comando c, int i) {
        return Parametro.normalizarOpcion(c.parametro(i));
    }

    private static Convenio convenio(int id, Comando c, int d) {
        return new Convenio(id, op(c, d), c.texto(d + 1), c.texto(d + 2), c.fecha(d + 3), c.fecha(d + 4),
                op(c, d + 5), c.texto(d + 6), c.texto(d + 7));
    }

    private static ServicioInnova servicio(int id, Comando c, int d) {
        return new ServicioInnova(id, c.texto(d), c.texto(d + 1));
    }

    private static MiembroInnova miembro(int id, Comando c, int d) {
        String correo = c.texto(d + 3);
        return new MiembroInnova(id, c.texto(d), c.texto(d + 1), c.texto(d + 2), correo == null ? null : correo.toLowerCase());
    }

    private static ProyectoInnova proyecto(int id, Comando c, int d) {
        return new ProyectoInnova(id, c.texto(d), c.texto(d + 1), c.texto(d + 2), c.entero(d + 3), c.texto(d + 4));
    }

    /** El correo del graduado es siempre el del remitente: nadie puede registrar ni cambiar los datos de otro. */
    private static Graduado graduado(String correo, Comando c) {
        return new Graduado(0, correo, c.texto(0), c.texto(1), c.texto(2), c.entero(3), c.texto(4), c.texto(5),
                op(c, 6), c.texto(7), null);
    }

    // ================================================================ Respuestas

    private Respuesta paginaInnova() throws SQLException, ErrorNegocio {
        InnovaNegocio.PaginaInnova pagina = innova.pagina();
        return Respuesta.exito("Empresa Junior INNOVA", "Información de la Empresa Junior INNOVA.")
                .agregar(new Parrafo("¿Quiénes somos?", pagina.descripcion()))
                .agregar(tablaServicios(pagina.servicios(), false))
                .agregar(tablaMiembros(pagina.equipo(), false))
                .agregar(tablaProyectos(pagina.proyectos(), false))
                .agregar(new Parrafo("¿Cómo solicitar un servicio o postular?",
                        "Empresas: SOLINN[\"SERVICIO\",\"Nombre de la empresa\",\"Teléfono\",\"Rubro\",\"Qué necesita\"]\n"
                                + "Estudiantes: SOLINN[\"POSTULACION\",\"Su nombre\",\"Teléfono\",\"Semestre\",\"Por qué quiere participar\"]\n"
                                + "Luego puede ver el estado con MISSOL."));
    }

    private Respuesta solicitar(Comando c, String correo) throws SQLException, ErrorNegocio {
        String tipo = op(c, 0);
        SolicitudInnova solicitud = new SolicitudInnova(0, tipo, c.texto(1), correo, c.texto(2), c.texto(3), c.texto(4),
                "PENDIENTE", null, null, null, null);
        int id = innova.solicitar(solicitud);
        String que = tipo.equals("SERVICIO") ? "solicitud de servicio" : "postulación";
        return insertado(que + " a INNOVA", id)
                .agregar(new Parrafo("Siguiente paso", "INNOVA revisará su " + que + " y le avisará por correo."
                        + " Mientras tanto puede ver el estado con MISSOL."));
    }

    private Respuesta atender(Comando c, String usuario) throws SQLException, ErrorNegocio {
        int id = c.entero(0);
        String estado = op(c, 1);
        String observacion = c.parametro(2);
        SolicitudInnova solicitud = innova.atender(usuario, id, estado, observacion);
        String que = solicitud.tipo().equals("SERVICIO") ? "solicitud de servicio" : "postulación";
        Respuesta aviso = Respuesta.aviso("Respuesta de INNOVA a su " + que,
                        "Su " + que + " N° " + id + " fue marcada como " + estado + ".")
                .agregar(new Parrafo("Observación de INNOVA", observacion))
                .agregar(new Parrafo("Su mensaje original", solicitud.mensaje()));
        return Respuesta.exito("Solicitud atendida", "Registro modificado correctamente (solicitud " + id + " de "
                        + solicitud.nombre() + " marcada como " + estado + ").")
                .agregar(new Parrafo("Aviso", "Se envía un correo a " + solicitud.correo() + " con la observación."))
                .avisar(solicitud.correo(), aviso);
    }

    private Respuesta fichaGraduado(Graduado g) {
        TablaHTML ficha = new TablaHTML("Mis datos de graduado", "Dato", "Valor").sinCantidad()
                .fila("Nombre", g.nombre() + " " + g.apellidos())
                .fila("Correo", g.correo())
                .fila("Teléfono", g.telefono())
                .fila("Año de titulación", g.anioTitulacion())
                .fila("Empleo actual", g.empleoActual())
                .fila("Cargo", g.cargo())
                .fila("Sector", g.sector())
                .fila("Ciudad", g.ciudad())
                .fila("Última actualización", Respuestas.fecha(g.actualizado()));
        return Respuesta.exito("Mis datos de graduado", "Registro encontrado. Para cambiarlo use ACTGRA.").agregar(ficha);
    }

    private Respuesta exportarGraduados(List<Graduado> lista) {
        ArchivoCSV csv = new ArchivoCSV("ID", "Nombre", "Apellidos", "Correo", "Teléfono", "Año de titulación",
                "Empleo actual", "Cargo", "Sector", "Ciudad", "Actualizado");
        for (Graduado g : lista) {
            csv.fila(g.id(), g.nombre(), g.apellidos(), g.correo(), g.telefono(), g.anioTitulacion(), g.empleoActual(),
                    g.cargo(), g.sector(), g.ciudad(), Respuestas.fecha(g.actualizado()));
        }
        String archivo = "graduados-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return Respuesta.exito("Exportación de graduados", lista.size() + " graduado(s) exportado(s) en el archivo adjunto "
                        + archivo + ".")
                .agregar(tablaGraduados(lista))
                .adjuntar(csv.comoAdjunto(archivo));
    }

    private static TablaHTML tablaConvenios(List<Convenio> lista) {
        TablaHTML t = new TablaHTML("Convenios y vinculación", "ID", "Tipo", "Institución", "Objeto", "Vigencia",
                "Estado", "Responsable", "Logotipo");
        for (Convenio v : lista) {
            String vigencia = Respuestas.fecha(v.fechaInicio()) + " al "
                    + (v.fechaFin() == null ? "(sin fecha)" : Respuestas.fecha(v.fechaFin()));
            t.fila(v.id(), v.tipo(), v.institucion(), v.objeto(), vigencia, v.estado(), v.responsable(), v.logoUrl());
        }
        return t;
    }

    private static TablaHTML tablaServicios(List<ServicioInnova> lista, boolean conId) {
        TablaHTML t = conId ? new TablaHTML("Servicios", "ID", "Servicio", "Descripción")
                : new TablaHTML("Servicios", "Servicio", "Descripción");
        for (ServicioInnova s : lista) {
            if (conId) {
                t.fila(s.id(), s.nombre(), s.descripcion());
            } else {
                t.fila(s.nombre(), s.descripcion());
            }
        }
        return t;
    }

    /** En la página pública no se muestran los correos de los estudiantes. */
    private static TablaHTML tablaMiembros(List<MiembroInnova> lista, boolean administrador) {
        TablaHTML t = administrador ? new TablaHTML("Equipo", "ID", "Nombre", "Cargo", "Semestre", "Correo")
                : new TablaHTML("Equipo", "Nombre", "Cargo", "Semestre");
        for (MiembroInnova m : lista) {
            if (administrador) {
                t.fila(m.id(), m.nombre(), m.cargo(), m.semestre(), m.correo());
            } else {
                t.fila(m.nombre(), m.cargo(), m.semestre());
            }
        }
        return t;
    }

    private static TablaHTML tablaProyectos(List<ProyectoInnova> lista, boolean conId) {
        TablaHTML t = conId ? new TablaHTML("Proyectos realizados", "ID", "Año", "Proyecto", "Cliente", "Descripción", "Resultado")
                : new TablaHTML("Proyectos realizados", "Año", "Proyecto", "Cliente", "Resultado");
        for (ProyectoInnova p : lista) {
            if (conId) {
                t.fila(p.id(), p.anio(), p.nombre(), p.cliente(), p.descripcion(), p.resultado());
            } else {
                t.fila(p.anio(), p.nombre(), p.cliente(), p.resultado());
            }
        }
        return t;
    }

    private static TablaHTML tablaSolicitudes(List<SolicitudInnova> lista, boolean administrador) {
        TablaHTML t = administrador
                ? new TablaHTML("Solicitudes", "ID", "Fecha", "Tipo", "Nombre", "Correo", "Teléfono", "Organización",
                "Mensaje", "Estado", "Observación")
                : new TablaHTML("Solicitudes", "N°", "Fecha", "Tipo", "Mensaje", "Estado", "Observación");
        for (SolicitudInnova s : lista) {
            if (administrador) {
                t.fila(s.id(), Respuestas.fecha(s.fecha()), s.tipo(), s.nombre(), s.correo(), s.telefono(),
                        s.organizacion(), s.mensaje(), s.estado(), s.observacion());
            } else {
                t.fila(s.id(), Respuestas.fecha(s.fecha()), s.tipo(), s.mensaje(), s.estado(), s.observacion());
            }
        }
        return t;
    }

    private static TablaHTML tablaGraduados(List<Graduado> lista) {
        TablaHTML t = new TablaHTML("Graduados", "ID", "Nombre", "Correo", "Teléfono", "Año", "Empleo actual", "Cargo",
                "Sector", "Ciudad");
        for (Graduado g : lista) {
            t.fila(g.id(), g.nombre() + " " + g.apellidos(), g.correo(), g.telefono(), g.anioTitulacion(),
                    g.empleoActual(), g.cargo(), g.sector(), g.ciudad());
        }
        return t;
    }
}
