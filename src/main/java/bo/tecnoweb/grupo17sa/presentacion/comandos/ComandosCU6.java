package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.modelo.Comunicado;
import bo.tecnoweb.grupo17sa.modelo.Evento;
import bo.tecnoweb.grupo17sa.modelo.Galeria;
import bo.tecnoweb.grupo17sa.modelo.Noticia;
import bo.tecnoweb.grupo17sa.negocio.AccesoNegocio;
import bo.tecnoweb.grupo17sa.negocio.ErrorNegocio;
import bo.tecnoweb.grupo17sa.negocio.NoticiasNegocio;
import bo.tecnoweb.grupo17sa.presentacion.lexico.Comando;
import bo.tecnoweb.grupo17sa.presentacion.vista.Parrafo;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import bo.tecnoweb.grupo17sa.presentacion.vista.TablaHTML;

import java.sql.SQLException;
import java.util.List;

import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.privado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.publico;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.entero;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.enteroOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.fecha;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.fechaOpcional;
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
 * CU6 Noticias, Comunicados, Eventos y Galería (RF-07, RF-08, RF-17, RF-18).
 */
public class ComandosCU6 implements ModuloComandos {

    private static final Parametro[] NOTICIA = {
            texto("titulo", 200),
            texto("resumen", 2000),
            texto("contenido", 5000),
            urlOpcional("imagen_url", 500),
            texto("autor", 150),
            entero("orden")
    };

    private static final Parametro[] COMUNICADO = {
            texto("titulo", 200),
            texto("contenido", 5000),
            opcion("prioridad", "BAJA", "MEDIA", "ALTA", "URGENTE"),
            fechaOpcional("fecha_expiracion")
    };

    private static final Parametro[] EVENTO = {
            texto("titulo", 200),
            texto("descripcion", 5000),
            fecha("fecha_inicio"),
            fechaOpcional("fecha_fin"),
            texto("lugar", 200),
            urlOpcional("imagen_url", 500)
    };

    private static final Parametro[] GALERIA = {
            texto("titulo", 200),
            textoOpcional("descripcion", 2000),
            url("imagen_url", 500),
            enteroOpcional("evento_id"),
            entero("orden")
    };

    private static final List<DefinicionComando> COMANDOS = List.of(
            // Noticias (RF-07)
            publico("LISNOT", "Lista las noticias publicadas (administradores ven todas)", filtro()),
            publico("VERNOT", "Muestra el detalle completo de una noticia", entero("id")),
            privado("INSNOT", "Registra una nueva noticia", NOTICIA),
            privado("MODNOT", "Modifica una noticia existente", conId(NOTICIA)),
            privado("ELINOT", "Elimina una noticia", entero("id")),

            // Comunicados (RF-08)
            publico("LISCOM", "Lista los comunicados y avisos vigentes (administradores ven todos)", filtro()),
            privado("INSCOM", "Registra un comunicado o aviso oficial", COMUNICADO),
            privado("MODCOM", "Modifica un comunicado existente", conId(COMUNICADO)),
            privado("ELICOM", "Elimina un comunicado", entero("id")),

            // Eventos (RF-17)
            publico("LISEVT", "Lista los eventos y actividades de la Carrera", filtro()),
            privado("INSEVT", "Registra un nuevo evento o actividad", EVENTO),
            privado("MODEVT", "Modifica un evento existente", conId(EVENTO)),
            privado("ELIEVT", "Elimina un evento", entero("id")),

            // Galería (RF-18)
            publico("LISGAL", "Lista las imágenes de la galería con sus eventos asociados", filtro()),
            privado("INSGAL", "Registra una nueva imagen en la galería", GALERIA),
            privado("MODGAL", "Modifica una imagen de la galería", conId(GALERIA)),
            privado("ELIGAL", "Elimina una imagen de la galería", entero("id"))
    );

    private final NoticiasNegocio noticias = new NoticiasNegocio();
    private final AccesoNegocio acceso = new AccesoNegocio();

    @Override
    public String codigo() {
        return "CU6";
    }

    @Override
    public String nombre() {
        return "Noticias, Comunicados, Eventos y Galería";
    }

    @Override
    public List<DefinicionComando> comandos() {
        return COMANDOS;
    }

    @Override
    public Respuesta ejecutar(Comando c, String usuario) throws SQLException, ErrorNegocio {
        switch (c.nombre()) {
            case "LISNOT": {
                boolean admin = esAdmin(usuario);
                List<Noticia> lista = admin ? noticias.listarTodasNoticias(c.parametro(0)) : noticias.listarNoticias(c.parametro(0));
                return listado("Noticias " + (admin ? "(Administración - Todas)" : "publicadas"), tablaNoticias(lista));
            }
            case "VERNOT": {
                Noticia n = noticias.buscarNoticia(c.entero(0));
                if (n == null) {
                    throw new ErrorNegocio("No existe la noticia con ID " + c.entero(0) + ".");
                }
                Respuesta r = Respuesta.exito("Noticia #" + n.id() + ": " + n.titulo(),
                        "Publicado por " + n.autor() + " el " + fecha(n.fechaPublicacion()) + (n.publicado() ? "" : " (Borrador)"))
                        .agregar(new Parrafo("Resumen", n.resumen()))
                        .agregar(new Parrafo("Contenido", n.contenido()));
                if (n.imagenUrl() != null && !n.imagenUrl().isBlank()) {
                    r.agregar(new Parrafo("Imagen", n.imagenUrl()));
                }
                return r;
            }
            case "INSNOT":
                exigirPermiso(usuario);
                return insertado("noticia", noticias.insertarNoticia(usuario, noticia(0, c, 0)));
            case "MODNOT":
                exigirPermiso(usuario);
                noticias.modificarNoticia(usuario, noticia(c.entero(0), c, 1));
                return modificado("noticia", c.entero(0));
            case "ELINOT": {
                exigirPermiso(usuario);
                Noticia borrada = noticias.eliminarNoticia(usuario, c.entero(0));
                return eliminado("noticia", c.entero(0), borrada.titulo());
            }

            case "LISCOM": {
                boolean admin = esAdmin(usuario);
                List<Comunicado> lista = admin ? noticias.listarTodosComunicados(c.parametro(0)) : noticias.listarComunicados(c.parametro(0));
                return listado("Comunicados " + (admin ? "(Administración - Todos)" : "vigentes"), tablaComunicados(lista));
            }
            case "INSCOM":
                exigirPermiso(usuario);
                return insertado("comunicado", noticias.insertarComunicado(usuario, comunicado(0, c, 0)));
            case "MODCOM":
                exigirPermiso(usuario);
                noticias.modificarComunicado(usuario, comunicado(c.entero(0), c, 1));
                return modificado("comunicado", c.entero(0));
            case "ELICOM": {
                exigirPermiso(usuario);
                Comunicado borrado = noticias.eliminarComunicado(usuario, c.entero(0));
                return eliminado("comunicado", c.entero(0), borrado.titulo());
            }

            case "LISEVT":
                return listado("Eventos y actividades", tablaEventos(noticias.listarEventos(c.parametro(0))));
            case "INSEVT":
                exigirPermiso(usuario);
                return insertado("evento", noticias.insertarEvento(usuario, evento(0, c, 0)));
            case "MODEVT":
                exigirPermiso(usuario);
                noticias.modificarEvento(usuario, evento(c.entero(0), c, 1));
                return modificado("evento", c.entero(0));
            case "ELIEVT": {
                exigirPermiso(usuario);
                Evento borrado = noticias.eliminarEvento(usuario, c.entero(0));
                return eliminado("evento", c.entero(0), borrado.titulo());
            }

            case "LISGAL":
                return listado("Galería de imágenes", tablaGaleria(noticias.listarGaleria(c.parametro(0))));
            case "INSGAL":
                exigirPermiso(usuario);
                return insertado("imagen de galería", noticias.insertarGaleria(usuario, galeria(0, c, 0)));
            case "MODGAL":
                exigirPermiso(usuario);
                noticias.modificarGaleria(usuario, galeria(c.entero(0), c, 1));
                return modificado("imagen de galería", c.entero(0));
            case "ELIGAL": {
                exigirPermiso(usuario);
                Galeria borrada = noticias.eliminarGaleria(usuario, c.entero(0));
                return eliminado("imagen de galería", c.entero(0), borrada.titulo());
            }

            default:
                throw new IllegalStateException("Comando sin implementar: " + c.nombre());
        }
    }

    private void exigirPermiso(String correo) throws ErrorNegocio {
        if (!esAdmin(correo)) {
            throw new ErrorNegocio("El correo " + correo + " no tiene permiso para administrar este módulo.");
        }
    }

    private boolean esAdmin(String correo) {
        return acceso.tienePermiso(correo, codigo(), "");
    }

    // =========================================================================
    // Armado de objetos desde los parámetros
    // =========================================================================

    private static Parametro[] conId(Parametro... resto) {
        Parametro[] todos = new Parametro[resto.length + 1];
        todos[0] = entero("id");
        System.arraycopy(resto, 0, todos, 1, resto.length);
        return todos;
    }

    private static Noticia noticia(int id, Comando c, int offset) {
        return new Noticia(id, c.texto(offset), c.texto(offset + 1), c.texto(offset + 2),
                c.texto(offset + 3), c.texto(offset + 4), c.entero(offset + 5));
    }

    private static Comunicado comunicado(int id, Comando c, int offset) {
        return new Comunicado(id, c.texto(offset), c.texto(offset + 1), c.parametro(offset + 2),
                c.fechaHoraOpcional(offset + 3));
    }

    private static Evento evento(int id, Comando c, int offset) {
        return new Evento(id, c.texto(offset), c.texto(offset + 1), c.fechaHora(offset + 2),
                c.fechaHoraOpcional(offset + 3), c.texto(offset + 4), c.texto(offset + 5));
    }

    private static Galeria galeria(int id, Comando c, int offset) {
        return new Galeria(id, c.texto(offset), c.texto(offset + 1), c.texto(offset + 2),
                c.enteroOpcional(offset + 3), c.entero(offset + 4));
    }

    // =========================================================================
    // Tablas HTML para respuestas
    // =========================================================================

    private static TablaHTML tablaNoticias(List<Noticia> lista) {
        TablaHTML t = new TablaHTML("Noticias", "ID", "Título", "Resumen", "Autor", "Publicación", "Publicado", "Orden");
        for (Noticia n : lista) {
            t.fila(n.id(), n.titulo(), n.resumen(), n.autor(), fecha(n.fechaPublicacion()),
                    n.publicado() ? "Sí" : "No", n.orden());
        }
        return t;
    }

    private static TablaHTML tablaComunicados(List<Comunicado> lista) {
        TablaHTML t = new TablaHTML("Comunicados", "ID", "Título", "Contenido", "Prioridad", "Publicación", "Expiración");
        for (Comunicado c : lista) {
            t.fila(c.id(), c.titulo(), c.contenido(), c.prioridad(), fecha(c.fechaPublicacion()),
                    c.fechaExpiracion() == null ? "Permanente" : fecha(c.fechaExpiracion()));
        }
        return t;
    }

    private static TablaHTML tablaEventos(List<Evento> lista) {
        TablaHTML t = new TablaHTML("Eventos", "ID", "Título", "Descripción", "Inicio", "Fin", "Lugar");
        for (Evento e : lista) {
            t.fila(e.id(), e.titulo(), e.descripcion(), fecha(e.fechaInicio()),
                    e.fechaFin() == null ? "-" : fecha(e.fechaFin()), e.lugar());
        }
        return t;
    }

    private static TablaHTML tablaGaleria(List<Galeria> lista) {
        TablaHTML t = new TablaHTML("Galería de imágenes", "ID", "Título", "Descripción", "Imagen", "Evento Asociado", "Orden");
        for (Galeria g : lista) {
            t.fila(g.id(), g.titulo(), g.descripcion() == null ? "" : g.descripcion(),
                    g.imagenUrl(), g.eventoNombre() == null ? "(Ninguno)" : g.eventoNombre(), g.orden());
        }
        return t;
    }
}
