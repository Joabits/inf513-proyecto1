package bo.tecnoweb.grupo17sa.negocio;

import bo.tecnoweb.grupo17sa.datos.ComunicadoDAO;
import bo.tecnoweb.grupo17sa.datos.EventoDAO;
import bo.tecnoweb.grupo17sa.datos.GaleriaDAO;
import bo.tecnoweb.grupo17sa.datos.NoticiaDAO;
import bo.tecnoweb.grupo17sa.modelo.Comunicado;
import bo.tecnoweb.grupo17sa.modelo.Evento;
import bo.tecnoweb.grupo17sa.modelo.Galeria;
import bo.tecnoweb.grupo17sa.modelo.Noticia;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * CU6 Noticias, Comunicados, Eventos y Galería.
 * Todas las operaciones de escritura se ejecutan dentro de Transaccion.ejecutar(...)
 * y se registran en BitacoraNegocio usando la misma conexión.
 */
public class NoticiasNegocio {

    private static final String MODULO = "CU6";
    private static final Set<String> PRIORIDADES = Set.of("BAJA", "MEDIA", "ALTA", "URGENTE");

    private final NoticiaDAO noticiaDAO = new NoticiaDAO();
    private final ComunicadoDAO comunicadoDAO = new ComunicadoDAO();
    private final EventoDAO eventoDAO = new EventoDAO();
    private final GaleriaDAO galeriaDAO = new GaleriaDAO();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    // =========================================================================
    // Noticias (noticia)
    // =========================================================================

    public List<Noticia> listarNoticias(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> noticiaDAO.listar(con, filtro));
    }

    public List<Noticia> listarTodasNoticias(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> noticiaDAO.listarTodas(con, filtro));
    }

    public Noticia buscarNoticia(int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> noticiaDAO.buscar(con, id));
    }

    public int insertarNoticia(String usuario, Noticia n) throws SQLException, ErrorNegocio {
        validarNoticia(n);
        LocalDateTime fechaPub = n.fechaPublicacion() != null ? n.fechaPublicacion() : LocalDateTime.now();
        Noticia paraGuardar = new Noticia(n.id(), n.titulo(), n.resumen(), n.contenido(),
                n.imagenUrl(), n.autor(), fechaPub, n.publicado(), n.orden(), n.creado());
        return Transaccion.ejecutar(con -> {
            int id = noticiaDAO.insertar(con, paraGuardar);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "noticia", id, n.titulo().trim());
            return id;
        });
    }

    public void modificarNoticia(String usuario, Noticia n) throws SQLException, ErrorNegocio {
        validarNoticia(n);
        Transaccion.ejecutar(con -> {
            Noticia existente = noticiaDAO.buscar(con, n.id());
            if (existente == null) {
                throw new ErrorNegocio("No existe la noticia con ID " + n.id() + ".");
            }
            noticiaDAO.modificar(con, n);
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "noticia", n.id(), n.titulo().trim());
            return null;
        });
    }

    public Noticia eliminarNoticia(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Noticia existente = noticiaDAO.buscar(con, id);
            if (existente == null) {
                throw new ErrorNegocio("No existe la noticia con ID " + id + ".");
            }
            noticiaDAO.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "noticia", id, existente.titulo());
            return existente;
        });
    }

    // =========================================================================
    // Comunicados (comunicado)
    // =========================================================================

    public List<Comunicado> listarComunicados(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> comunicadoDAO.listar(con, filtro));
    }

    public List<Comunicado> listarTodosComunicados(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> comunicadoDAO.listarTodos(con, filtro));
    }

    public Comunicado buscarComunicado(int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> comunicadoDAO.buscar(con, id));
    }

    public int insertarComunicado(String usuario, Comunicado c) throws SQLException, ErrorNegocio {
        String prio = normalizarPrioridad(c.prioridad());
        validarComunicado(c);
        LocalDateTime fechaPub = c.fechaPublicacion() != null ? c.fechaPublicacion() : LocalDateTime.now();
        if (c.fechaExpiracion() != null && !c.fechaExpiracion().isAfter(fechaPub)) {
            throw new ErrorNegocio("La fecha de expiración debe ser posterior a la fecha de publicación.");
        }
        Comunicado paraGuardar = new Comunicado(c.id(), c.titulo(), c.contenido(), prio,
                fechaPub, c.fechaExpiracion(), c.creado());
        return Transaccion.ejecutar(con -> {
            int id = comunicadoDAO.insertar(con, paraGuardar);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "comunicado", id, c.titulo().trim());
            return id;
        });
    }

    public void modificarComunicado(String usuario, Comunicado c) throws SQLException, ErrorNegocio {
        String prio = normalizarPrioridad(c.prioridad());
        validarComunicado(c);
        Transaccion.ejecutar(con -> {
            Comunicado existente = comunicadoDAO.buscar(con, c.id());
            if (existente == null) {
                throw new ErrorNegocio("No existe el comunicado con ID " + c.id() + ".");
            }
            LocalDateTime fechaPub = existente.fechaPublicacion() != null ? existente.fechaPublicacion() : LocalDateTime.now();
            if (c.fechaExpiracion() != null && !c.fechaExpiracion().isAfter(fechaPub)) {
                throw new ErrorNegocio("La fecha de expiración debe ser posterior a la fecha de publicación.");
            }
            Comunicado paraGuardar = new Comunicado(c.id(), c.titulo(), c.contenido(), prio,
                    existente.fechaPublicacion(), c.fechaExpiracion(), existente.creado());
            comunicadoDAO.modificar(con, paraGuardar);
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "comunicado", c.id(), c.titulo().trim());
            return null;
        });
    }

    public Comunicado eliminarComunicado(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Comunicado existente = comunicadoDAO.buscar(con, id);
            if (existente == null) {
                throw new ErrorNegocio("No existe el comunicado con ID " + id + ".");
            }
            comunicadoDAO.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "comunicado", id, existente.titulo());
            return existente;
        });
    }

    // =========================================================================
    // Eventos (evento)
    // =========================================================================

    public List<Evento> listarEventos(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> eventoDAO.listar(con, filtro));
    }

    public Evento buscarEvento(int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> eventoDAO.buscar(con, id));
    }

    public int insertarEvento(String usuario, Evento e) throws SQLException, ErrorNegocio {
        validarEvento(e);
        return Transaccion.ejecutar(con -> {
            int id = eventoDAO.insertar(con, e);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "evento", id, e.titulo().trim());
            return id;
        });
    }

    public void modificarEvento(String usuario, Evento e) throws SQLException, ErrorNegocio {
        validarEvento(e);
        Transaccion.ejecutar(con -> {
            Evento existente = eventoDAO.buscar(con, e.id());
            if (existente == null) {
                throw new ErrorNegocio("No existe el evento con ID " + e.id() + ".");
            }
            eventoDAO.modificar(con, e);
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "evento", e.id(), e.titulo().trim());
            return null;
        });
    }

    public Evento eliminarEvento(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Evento existente = eventoDAO.buscar(con, id);
            if (existente == null) {
                throw new ErrorNegocio("No existe el evento con ID " + id + ".");
            }
            eventoDAO.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "evento", id, existente.titulo());
            return existente;
        });
    }

    // =========================================================================
    // Galería (galeria)
    // =========================================================================

    public List<Galeria> listarGaleria(String filtro) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> galeriaDAO.listar(con, filtro));
    }

    public Galeria buscarGaleria(int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> galeriaDAO.buscar(con, id));
    }

    public int insertarGaleria(String usuario, Galeria g) throws SQLException, ErrorNegocio {
        validarGaleria(g);
        return Transaccion.ejecutar(con -> {
            if (g.eventoId() != null && g.eventoId() > 0 && eventoDAO.buscar(con, g.eventoId()) == null) {
                throw new ErrorNegocio("No existe ningún evento con ID " + g.eventoId() + ".");
            }
            int id = galeriaDAO.insertar(con, g);
            bitacora.registrar(con, usuario, BitacoraNegocio.CREAR, MODULO, "galeria", id, g.titulo().trim());
            return id;
        });
    }

    public void modificarGaleria(String usuario, Galeria g) throws SQLException, ErrorNegocio {
        validarGaleria(g);
        Transaccion.ejecutar(con -> {
            Galeria existente = galeriaDAO.buscar(con, g.id());
            if (existente == null) {
                throw new ErrorNegocio("No existe la imagen de galería con ID " + g.id() + ".");
            }
            if (g.eventoId() != null && g.eventoId() > 0 && eventoDAO.buscar(con, g.eventoId()) == null) {
                throw new ErrorNegocio("No existe ningún evento con ID " + g.eventoId() + ".");
            }
            galeriaDAO.modificar(con, g);
            bitacora.registrar(con, usuario, BitacoraNegocio.MODIFICAR, MODULO, "galeria", g.id(), g.titulo().trim());
            return null;
        });
    }

    public Galeria eliminarGaleria(String usuario, int id) throws SQLException, ErrorNegocio {
        return Transaccion.ejecutar(con -> {
            Galeria existente = galeriaDAO.buscar(con, id);
            if (existente == null) {
                throw new ErrorNegocio("No existe la imagen de galería con ID " + id + ".");
            }
            galeriaDAO.eliminar(con, id);
            bitacora.registrar(con, usuario, BitacoraNegocio.ELIMINAR, MODULO, "galeria", id, existente.titulo());
            return existente;
        });
    }

    // =========================================================================
    // Validaciones
    // =========================================================================

    private static void validarNoticia(Noticia n) throws ErrorNegocio {
        if (n.titulo() == null || n.titulo().isBlank()) {
            throw new ErrorNegocio("El título de la noticia no puede estar vacío.");
        }
        if (n.resumen() == null || n.resumen().isBlank()) {
            throw new ErrorNegocio("El resumen de la noticia no puede estar vacío.");
        }
        if (n.contenido() == null || n.contenido().isBlank()) {
            throw new ErrorNegocio("El contenido de la noticia no puede estar vacío.");
        }
        if (n.autor() == null || n.autor().isBlank()) {
            throw new ErrorNegocio("El autor de la noticia no puede estar vacío.");
        }
        if (n.orden() <= 0) {
            throw new ErrorNegocio("El orden debe ser mayor a cero (se recibió " + n.orden() + ").");
        }
    }

    private static String normalizarPrioridad(String prioridad) throws ErrorNegocio {
        if (prioridad == null || prioridad.isBlank()) {
            throw new ErrorNegocio("La prioridad no puede estar vacía.");
        }
        String p = prioridad.trim().toUpperCase(Locale.ROOT);
        if (!PRIORIDADES.contains(p)) {
            throw new ErrorNegocio("La prioridad debe ser BAJA, MEDIA, ALTA o URGENTE (se recibió «" + prioridad + "»).");
        }
        return p;
    }

    private static void validarComunicado(Comunicado c) throws ErrorNegocio {
        if (c.titulo() == null || c.titulo().isBlank()) {
            throw new ErrorNegocio("El título del comunicado no puede estar vacío.");
        }
        if (c.contenido() == null || c.contenido().isBlank()) {
            throw new ErrorNegocio("El contenido del comunicado no puede estar vacío.");
        }
    }

    private static void validarEvento(Evento e) throws ErrorNegocio {
        if (e.titulo() == null || e.titulo().isBlank()) {
            throw new ErrorNegocio("El título del evento no puede estar vacío.");
        }
        if (e.descripcion() == null || e.descripcion().isBlank()) {
            throw new ErrorNegocio("La descripción del evento no puede estar vacía.");
        }
        if (e.lugar() == null || e.lugar().isBlank()) {
            throw new ErrorNegocio("El lugar del evento no puede estar vacío.");
        }
        if (e.fechaInicio() == null) {
            throw new ErrorNegocio("La fecha de inicio del evento es obligatoria.");
        }
        if (e.fechaFin() != null && !e.fechaFin().isAfter(e.fechaInicio())) {
            throw new ErrorNegocio("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
    }

    private static void validarGaleria(Galeria g) throws ErrorNegocio {
        if (g.titulo() == null || g.titulo().isBlank()) {
            throw new ErrorNegocio("El título de la imagen no puede estar vacío.");
        }
        if (g.imagenUrl() == null || g.imagenUrl().isBlank()) {
            throw new ErrorNegocio("La URL de la imagen no puede estar vacía.");
        }
        if (g.orden() <= 0) {
            throw new ErrorNegocio("El orden debe ser mayor a cero (se recibió " + g.orden() + ").");
        }
    }
}
