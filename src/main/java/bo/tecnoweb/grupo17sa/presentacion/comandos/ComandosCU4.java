package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.modelo.ArticuloRevista;
import bo.tecnoweb.grupo17sa.modelo.Documento;
import bo.tecnoweb.grupo17sa.modelo.EdicionRevista;
import bo.tecnoweb.grupo17sa.modelo.LineaInvestigacion;
import bo.tecnoweb.grupo17sa.modelo.ProyectoInvestigacion;
import bo.tecnoweb.grupo17sa.negocio.AccesoNegocio;
import bo.tecnoweb.grupo17sa.negocio.DocumentosNegocio;
import bo.tecnoweb.grupo17sa.negocio.ErrorNegocio;
import bo.tecnoweb.grupo17sa.negocio.InvestigacionNegocio;
import bo.tecnoweb.grupo17sa.negocio.RevistaNegocio;
import bo.tecnoweb.grupo17sa.presentacion.lexico.Comando;
import bo.tecnoweb.grupo17sa.presentacion.vista.Parrafo;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import bo.tecnoweb.grupo17sa.presentacion.vista.TablaHTML;

import java.sql.SQLException;
import java.util.List;

import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.privado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.publico;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.booleano;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.entero;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.filtro;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.opcion;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.texto;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.textoOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.url;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.urlOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.eliminado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.fecha;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.insertado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.listado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.modificado;

/**
 * CU4 Investigaciones, revista INNOVAR y repositorio documental (RF-06, RF-07 y RF-08). Los listados y las consultas
 * son públicos y muestran solo lo publicado; quien administra el CU4 ve también lo oculto y los borradores.
 */
public class ComandosCU4 implements ModuloComandos {

    // Parámetros que se repiten en INS y MOD (los mismos valores del CHECK de cada tabla)
    private static final Parametro[] LINEA = {texto("nombre", 160), textoOpcional("descripcion", 1000)};
    private static final Parametro[] PROYECTO = {
            texto("titulo", 200), opcion("tipo", "INVESTIGACION", "AULA", "SEMILLERO"), textoOpcional("linea_id", 10),
            texto("responsable", 160), textoOpcional("integrantes", 500), entero("gestion"),
            opcion("estado", "EN_CURSO", "CONCLUIDO"), texto("resumen", 1000)
    };
    private static final Parametro[] DOCUMENTO = {
            texto("titulo", 160), textoOpcional("descripcion", 300),
            opcion("categoria", "REGLAMENTO", "RESOLUCION", "FORMULARIO", "PLAN_DE_ESTUDIOS", "INFORME", "OTRO"),
            url("enlace", 500)
    };
    private static final Parametro[] EDICION = {
            textoOpcional("volumen", 2), entero("numero"), entero("anio"), textoOpcional("titulo", 160),
            textoOpcional("descripcion", 1000), urlOpcional("enlace_pdf", 500)
    };
    private static final Parametro[] ARTICULO = {
            texto("titulo", 200), texto("autores", 300), textoOpcional("resumen", 1000), urlOpcional("enlace_pdf", 500)
    };

    private static final List<DefinicionComando> COMANDOS = List.of(
            // ---------------------------------------------------- RF-07 Investigación
            publico("LISLIN", "Lista las líneas de investigación con su cantidad de proyectos publicados", filtro()),
            privado("INSLIN", "Registra una línea de investigación", LINEA),
            privado("MODLIN", "Modifica una línea de investigación", conId(LINEA)),
            privado("ELILIN", "Elimina una línea de investigación sin proyectos", entero("id")),
            publico("LISPRO", "Lista los proyectos de investigación publicados (filtro: título, responsable, línea, tipo...)",
                    filtro()),
            publico("VERPRO", "Muestra un proyecto de investigación completo", entero("id")),
            privado("INSPRO", "Registra un proyecto. tipo: INVESTIGACION, AULA (PIA) o SEMILLERO; linea_id: número de LISLIN"
                    + " o vacío; estado: EN_CURSO o CONCLUIDO. Queda publicado", PROYECTO),
            privado("MODPRO", "Modifica un proyecto de investigación", conId(PROYECTO)),
            privado("PUBPRO", "Publica (SI) u oculta (NO) un proyecto", entero("id"), booleano("publicado")),
            privado("ELIPRO", "Elimina un proyecto de investigación", entero("id")),
            // ---------------------------------------------------- RF-06 Repositorio de documentos
            publico("LISREP", "Lista los documentos publicados del repositorio, con el enlace al archivo", filtro()),
            privado("INSREP", "Registra un documento: categoría REGLAMENTO, RESOLUCION, FORMULARIO, PLAN_DE_ESTUDIOS,"
                    + " INFORME u OTRO; enlace al archivo en el servidor. Queda publicado", DOCUMENTO),
            privado("MODREP", "Modifica un documento", conId(DOCUMENTO)),
            privado("PUBREP", "Publica (SI) u oculta (NO) un documento", entero("id"), booleano("publicado")),
            privado("ELIREP", "Elimina un documento", entero("id")),
            // ---------------------------------------------------- RF-08 Revista INNOVAR
            publico("LISREV", "Lista las ediciones publicadas de la revista INNOVAR", filtro()),
            publico("VERREV", "Muestra una edición de la revista con sus artículos", entero("id")),
            privado("INSREV", "Registra una edición (volumen opcional: \"\"). Queda en borrador hasta publicarla con PUBREV",
                    EDICION),
            privado("MODREV", "Modifica una edición", conId(EDICION)),
            privado("PUBREV", "Publica (SI) u oculta (NO) una edición", entero("id"), booleano("publicada")),
            privado("ELIREV", "Elimina una edición y todos sus artículos", entero("id")),
            privado("INSART", "Agrega un artículo al final de una edición (número de LISREV)", conNombre("edicion", ARTICULO)),
            privado("MODART", "Modifica un artículo", conId(ARTICULO)),
            privado("ELIART", "Elimina un artículo", entero("id"))
    );

    private final InvestigacionNegocio investigacion = new InvestigacionNegocio();
    private final DocumentosNegocio documentos = new DocumentosNegocio();
    private final RevistaNegocio revista = new RevistaNegocio();
    private final AccesoNegocio acceso = new AccesoNegocio();

    @Override
    public String codigo() {
        return InvestigacionNegocio.MODULO;
    }

    @Override
    public String nombre() {
        return "Investigaciones, Revista INNOVAR y Repositorio Documental";
    }

    @Override
    public List<DefinicionComando> comandos() {
        return COMANDOS;
    }

    @Override
    public Respuesta ejecutar(Comando c, String usuario) throws SQLException, ErrorNegocio {
        switch (c.nombre()) {
            // ---------------------------------------------------- RF-07
            case "LISLIN":
                return listado("Líneas de investigación", tablaLineas(investigacion.listarLineas(c.parametro(0))));
            case "INSLIN":
                return insertado("línea de investigación", investigacion.insertarLinea(usuario, c.parametro(0), c.texto(1)));
            case "MODLIN":
                investigacion.modificarLinea(usuario, c.entero(0), c.parametro(1), c.texto(2));
                return modificado("línea de investigación", c.entero(0));
            case "ELILIN":
                return eliminado("línea de investigación", c.entero(0), investigacion.eliminarLinea(usuario, c.entero(0)).nombre());
            case "LISPRO": {
                boolean admin = administra(usuario, c);
                return listado("Proyectos de investigación", tablaProyectos(investigacion.listarProyectos(c.parametro(0), admin), admin));
            }
            case "VERPRO":
                return verProyecto(investigacion.verProyecto(c.entero(0), administra(usuario, c)));
            case "INSPRO":
                return insertado("proyecto de investigación", investigacion.insertarProyecto(usuario, proyecto(0, c, 0)));
            case "MODPRO":
                investigacion.modificarProyecto(usuario, proyecto(c.entero(0), c, 1));
                return modificado("proyecto de investigación", c.entero(0));
            case "PUBPRO":
                return publicacion("proyecto", c.entero(0), c.booleano(1),
                        investigacion.publicarProyecto(usuario, c.entero(0), c.booleano(1)).titulo());
            case "ELIPRO":
                return eliminado("proyecto de investigación", c.entero(0), investigacion.eliminarProyecto(usuario, c.entero(0)).titulo());

            // ---------------------------------------------------- RF-06
            case "LISREP": {
                boolean admin = administra(usuario, c);
                return listado("Repositorio de documentos", tablaDocumentos(documentos.listar(c.parametro(0), admin), admin));
            }
            case "INSREP":
                return insertado("documento", documentos.insertar(usuario, documento(0, c, 0)));
            case "MODREP":
                documentos.modificar(usuario, documento(c.entero(0), c, 1));
                return modificado("documento", c.entero(0));
            case "PUBREP":
                return publicacion("documento", c.entero(0), c.booleano(1),
                        documentos.publicar(usuario, c.entero(0), c.booleano(1)).titulo());
            case "ELIREP":
                return eliminado("documento", c.entero(0), documentos.eliminar(usuario, c.entero(0)).titulo());

            // ---------------------------------------------------- RF-08
            case "LISREV": {
                boolean admin = administra(usuario, c);
                return listado("Revista INNOVAR", tablaEdiciones(revista.listar(c.parametro(0), admin), admin));
            }
            case "VERREV":
                return verEdicion(revista.ver(c.entero(0), administra(usuario, c)));
            case "INSREV":
                return insertado("edición de la revista", revista.insertarEdicion(usuario, edicion(0, c, 0)))
                        .agregar(new Parrafo("Siguiente paso", "La edición quedó en borrador: agregue sus artículos con INSART"
                                + " y publíquela con PUBREV."));
            case "MODREV":
                revista.modificarEdicion(usuario, edicion(c.entero(0), c, 1));
                return modificado("edición de la revista", c.entero(0));
            case "PUBREV":
                return publicacion("edición", c.entero(0), c.booleano(1),
                        revista.publicarEdicion(usuario, c.entero(0), c.booleano(1)).nombre());
            case "ELIREV":
                return eliminado("edición de la revista", c.entero(0), revista.eliminarEdicion(usuario, c.entero(0)).nombre());
            case "INSART":
                return insertado("artículo", revista.insertarArticulo(usuario, articulo(0, c.entero(0), c, 1)));
            case "MODART":
                revista.modificarArticulo(usuario, articulo(c.entero(0), 0, c, 1));
                return modificado("artículo", c.entero(0));
            case "ELIART":
                return eliminado("artículo", c.entero(0), revista.eliminarArticulo(usuario, c.entero(0)).titulo());
            default:
                throw new IllegalStateException("Comando sin implementar: " + c.nombre());
        }
    }

    /** ¿El remitente administra el CU4? Entonces los listados públicos le muestran también lo oculto. */
    private boolean administra(String usuario, Comando c) {
        return acceso.tienePermiso(usuario, InvestigacionNegocio.MODULO, c.nombre());
    }

    private static Respuesta publicacion(String que, int id, boolean publicado, String nombre) {
        return Respuesta.exito(publicado ? "Publicado" : "Oculto", "Registro modificado correctamente (" + que + " " + id
                + ": «" + nombre + "» " + (publicado ? "ahora es visible para todos" : "ya no se muestra al público") + ").");
    }

    // ================================================================ Armado de objetos desde los parámetros

    private static Parametro[] conId(Parametro... resto) {
        return conNombre("id", resto);
    }

    /** Un parámetro entero con ese nombre seguido de los demás. */
    private static Parametro[] conNombre(String nombre, Parametro... resto) {
        Parametro[] todos = new Parametro[resto.length + 1];
        todos[0] = entero(nombre);
        System.arraycopy(resto, 0, todos, 1, resto.length);
        return todos;
    }

    /** Valor de una opción en mayúsculas y sin tildes ("en curso" → "EN_CURSO"). */
    private static String op(Comando c, int i) {
        return Parametro.normalizarOpcion(c.parametro(i));
    }

    private static ProyectoInvestigacion proyecto(int id, Comando c, int d) throws ErrorNegocio {
        return new ProyectoInvestigacion(id, c.parametro(d).trim(), op(c, d + 1), InvestigacionNegocio.leerLinea(c.texto(d + 2)),
                null, c.parametro(d + 3).trim(), c.texto(d + 4), c.entero(d + 5), op(c, d + 6), c.parametro(d + 7).trim(),
                true, null);
    }

    private static Documento documento(int id, Comando c, int d) {
        return new Documento(id, c.parametro(d).trim(), c.texto(d + 1), op(c, d + 2), c.parametro(d + 3).trim(), true, null);
    }

    private static EdicionRevista edicion(int id, Comando c, int d) throws ErrorNegocio {
        return new EdicionRevista(id, RevistaNegocio.leerVolumen(c.texto(d)), c.entero(d + 1), c.entero(d + 2), c.texto(d + 3),
                c.texto(d + 4), c.texto(d + 5), false, 0);
    }

    private static ArticuloRevista articulo(int id, int edicion, Comando c, int d) {
        return new ArticuloRevista(id, edicion, c.parametro(d).trim(), c.parametro(d + 1).trim(), c.texto(d + 2), c.texto(d + 3), 0);
    }

    // ================================================================ Tablas de las respuestas

    private static TablaHTML tablaLineas(List<LineaInvestigacion> lista) {
        TablaHTML t = new TablaHTML("Líneas de investigación", "ID", "Línea", "Descripción", "Proyectos");
        for (LineaInvestigacion l : lista) {
            t.fila(l.id(), l.nombre(), l.descripcion() == null ? "" : l.descripcion(), l.proyectos());
        }
        return t;
    }

    private static TablaHTML tablaProyectos(List<ProyectoInvestigacion> lista, boolean admin) {
        TablaHTML t = admin
                ? new TablaHTML("Proyectos de investigación", "ID", "Título", "Tipo", "Línea", "Responsable", "Gestión", "Estado", "Publicado")
                : new TablaHTML("Proyectos de investigación", "ID", "Título", "Tipo", "Línea", "Responsable", "Gestión", "Estado");
        for (ProyectoInvestigacion p : lista) {
            String linea = p.linea() == null ? "" : p.linea();
            if (admin) {
                t.fila(p.id(), p.titulo(), p.tipo(), linea, p.responsable(), p.gestion(), p.estado(), p.publicado() ? "Sí" : "No");
            } else {
                t.fila(p.id(), p.titulo(), p.tipo(), linea, p.responsable(), p.gestion(), p.estado());
            }
        }
        return t;
    }

    private static Respuesta verProyecto(ProyectoInvestigacion p) {
        TablaHTML t = new TablaHTML("Proyecto N° " + p.id(), "Dato", "Valor").sinCantidad()
                .fila("Título", p.titulo())
                .fila("Tipo", p.tipo())
                .fila("Línea de investigación", p.linea() == null ? "" : p.linea())
                .fila("Responsable", p.responsable())
                .fila("Integrantes", p.integrantes() == null ? "" : p.integrantes())
                .fila("Gestión", p.gestion())
                .fila("Estado", p.estado());
        if (!p.publicado()) {
            t.fila("Publicado", "No (solo lo ven quienes administran el CU4)");
        }
        return Respuesta.exito("Proyecto de investigación", p.titulo()).agregar(t).agregar(new Parrafo("Resumen", p.resumen()));
    }

    private static TablaHTML tablaDocumentos(List<Documento> lista, boolean admin) {
        TablaHTML t = admin
                ? new TablaHTML("Repositorio de documentos", "ID", "Título", "Categoría", "Descripción", "Enlace", "Publicado", "Registrado")
                : new TablaHTML("Repositorio de documentos", "ID", "Título", "Categoría", "Descripción", "Enlace");
        for (Documento d : lista) {
            String descripcion = d.descripcion() == null ? "" : d.descripcion();
            if (admin) {
                t.fila(d.id(), d.titulo(), d.categoria(), descripcion, d.enlace(), d.publicado() ? "Sí" : "No", fecha(d.creado()));
            } else {
                t.fila(d.id(), d.titulo(), d.categoria(), descripcion, d.enlace());
            }
        }
        return t;
    }

    private static TablaHTML tablaEdiciones(List<EdicionRevista> lista, boolean admin) {
        TablaHTML t = admin
                ? new TablaHTML("Ediciones de la revista INNOVAR", "ID", "Edición", "Título", "Artículos", "PDF", "Publicada")
                : new TablaHTML("Ediciones de la revista INNOVAR", "ID", "Edición", "Título", "Artículos", "PDF");
        for (EdicionRevista e : lista) {
            String titulo = e.titulo() == null ? "" : e.titulo();
            String pdf = e.enlacePdf() == null ? "" : e.enlacePdf();
            if (admin) {
                t.fila(e.id(), e.nombre(), titulo, e.articulos(), pdf, e.publicada() ? "Sí" : "No (borrador)");
            } else {
                t.fila(e.id(), e.nombre(), titulo, e.articulos(), pdf);
            }
        }
        return t;
    }

    private static Respuesta verEdicion(RevistaNegocio.EdicionCompleta completa) {
        EdicionRevista e = completa.edicion();
        Respuesta respuesta = Respuesta.exito("Revista INNOVAR", e.nombre() + (e.titulo() == null ? "" : " — " + e.titulo())
                + (e.publicada() ? "" : " (borrador: todavía no se publica)"));
        if (e.descripcion() != null) {
            respuesta.agregar(new Parrafo("Presentación", e.descripcion()));
        }
        if (e.enlacePdf() != null) {
            respuesta.agregar(new Parrafo("Edición completa (PDF)", e.enlacePdf()));
        }
        TablaHTML t = new TablaHTML("Artículos", "ID", "Título", "Autores", "PDF");
        for (ArticuloRevista a : completa.articulos()) {
            t.fila(a.id(), a.titulo(), a.autores(), a.enlacePdf() == null ? "" : a.enlacePdf());
        }
        return respuesta.agregar(t);
    }
}
