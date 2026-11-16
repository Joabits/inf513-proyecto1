package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.modelo.AccesoRapido;
import bo.tecnoweb.grupo17sa.modelo.Banner;
import bo.tecnoweb.grupo17sa.modelo.DatoConfiguracion;
import bo.tecnoweb.grupo17sa.modelo.Destacado;
import bo.tecnoweb.grupo17sa.modelo.RedSocial;
import bo.tecnoweb.grupo17sa.modelo.RegistroBitacora;
import bo.tecnoweb.grupo17sa.modelo.SeccionInstitucional;
import bo.tecnoweb.grupo17sa.negocio.BitacoraNegocio;
import bo.tecnoweb.grupo17sa.negocio.ContactoNegocio;
import bo.tecnoweb.grupo17sa.negocio.ErrorNegocio;
import bo.tecnoweb.grupo17sa.negocio.InicioNegocio;
import bo.tecnoweb.grupo17sa.negocio.InstitucionalNegocio;
import bo.tecnoweb.grupo17sa.presentacion.lexico.Comando;
import bo.tecnoweb.grupo17sa.presentacion.vista.Parrafo;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import bo.tecnoweb.grupo17sa.presentacion.vista.TablaHTML;

import java.sql.SQLException;
import java.util.List;

import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.privado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.publico;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.entero;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.filtro;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.texto;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.textoOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.url;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.urlOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.eliminado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.fecha;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.insertado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.listado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.modificado;

/** CU2 Portal Web y Bitácora de Auditoría (RF-01, RF-02, RF-16 y RF-19). */
public class ComandosCU2 implements ModuloComandos {

    private static final List<DefinicionComando> COMANDOS = List.of(
            // RF-01 Página de inicio
            publico("INICIO", "Página de inicio: bienvenida, banners, contenidos destacados y accesos rápidos"),
            privado("LISBAN", "Lista los banners del carrusel", filtro()),
            privado("INSBAN", "Registra un banner del carrusel",
                    texto("titulo", 150), textoOpcional("subtitulo", 250), url("imagen_url", 500),
                    urlOpcional("enlace", 500), entero("orden")),
            privado("MODBAN", "Modifica un banner",
                    entero("id"), texto("titulo", 150), textoOpcional("subtitulo", 250), url("imagen_url", 500),
                    urlOpcional("enlace", 500), entero("orden")),
            privado("ELIBAN", "Elimina un banner", entero("id")),
            privado("LISDES", "Lista los contenidos destacados", filtro()),
            privado("INSDES", "Registra un contenido destacado",
                    texto("titulo", 150), texto("descripcion", 500), urlOpcional("enlace", 500), entero("orden")),
            privado("MODDES", "Modifica un contenido destacado",
                    entero("id"), texto("titulo", 150), texto("descripcion", 500), urlOpcional("enlace", 500), entero("orden")),
            privado("ELIDES", "Elimina un contenido destacado", entero("id")),
            privado("LISACC", "Lista los accesos rápidos", filtro()),
            privado("INSACC", "Registra un acceso rápido a una plataforma de la Universidad",
                    texto("nombre", 100), url("url", 500), textoOpcional("descripcion", 250), entero("orden")),
            privado("MODACC", "Modifica un acceso rápido",
                    entero("id"), texto("nombre", 100), url("url", 500), textoOpcional("descripcion", 250), entero("orden")),
            privado("ELIACC", "Elimina un acceso rápido", entero("id")),

            // RF-02 Información institucional
            publico("LISINT", "Lista las secciones de información institucional", filtro()),
            publico("VERINT", "Muestra una sección: historia, mision, vision, objetivos, perfil_profesional, "
                    + "campo_ocupacional o estructura_organizativa", texto("clave", 40)),
            privado("MODINT", "Modifica el título y el contenido de una sección institucional",
                    texto("clave", 40), texto("titulo", 150), texto("contenido", 5000)),

            // RF-16 Contacto y redes sociales
            publico("CONTACTO", "Dirección, mapa, teléfonos, correos, horario y redes sociales de la Carrera"),
            privado("LISCFG", "Lista los datos configurables del portal (bienvenida y contacto)", filtro()),
            privado("MODCFG", "Modifica un dato configurable; las claves se ven con LISCFG",
                    texto("clave", 40), textoOpcional("valor", 2000)),
            privado("LISRED", "Lista las redes sociales", filtro()),
            privado("INSRED", "Registra una red social", texto("nombre", 50), url("url", 500), entero("orden")),
            privado("MODRED", "Modifica una red social",
                    entero("id"), texto("nombre", 50), url("url", 500), entero("orden")),
            privado("ELIRED", "Elimina una red social", entero("id")),

            // RF-19 Bitácora de actividad
            privado("LISBIT", "Consulta la bitácora: \"*\" (todo), un correo o texto, o una fecha AAAA-MM-DD", filtro())
    );

    private final InicioNegocio inicio = new InicioNegocio();
    private final InstitucionalNegocio institucional = new InstitucionalNegocio();
    private final ContactoNegocio contacto = new ContactoNegocio();
    private final BitacoraNegocio bitacora = new BitacoraNegocio();

    @Override
    public String codigo() {
        return "CU2";
    }

    @Override
    public String nombre() {
        return "Portal Web y Bitácora de Auditoría";
    }

    @Override
    public List<DefinicionComando> comandos() {
        return COMANDOS;
    }

    @Override
    public Respuesta ejecutar(Comando c, String usuario) throws SQLException, ErrorNegocio {
        switch (c.nombre()) {
            // ---------------------------------------------------- RF-01
            case "INICIO":
                return paginaInicio();
            case "LISBAN":
                return listado("Banners", tablaBanners(inicio.listarBanners(c.parametro(0))));
            case "INSBAN":
                return insertado("banner", inicio.insertarBanner(usuario, banner(0, c, 0)));
            case "MODBAN":
                inicio.modificarBanner(usuario, banner(c.entero(0), c, 1));
                return modificado("banner", c.entero(0));
            case "ELIBAN":
                return eliminado("banner", c.entero(0), inicio.eliminarBanner(usuario, c.entero(0)).titulo());
            case "LISDES":
                return listado("Contenidos destacados", tablaDestacados(inicio.listarDestacados(c.parametro(0))));
            case "INSDES":
                return insertado("contenido destacado", inicio.insertarDestacado(usuario, destacado(0, c, 0)));
            case "MODDES":
                inicio.modificarDestacado(usuario, destacado(c.entero(0), c, 1));
                return modificado("contenido destacado", c.entero(0));
            case "ELIDES":
                return eliminado("contenido destacado", c.entero(0), inicio.eliminarDestacado(usuario, c.entero(0)).titulo());
            case "LISACC":
                return listado("Accesos rápidos", tablaAccesos(inicio.listarAccesos(c.parametro(0))));
            case "INSACC":
                return insertado("acceso rápido", inicio.insertarAcceso(usuario, acceso(0, c, 0)));
            case "MODACC":
                inicio.modificarAcceso(usuario, acceso(c.entero(0), c, 1));
                return modificado("acceso rápido", c.entero(0));
            case "ELIACC":
                return eliminado("acceso rápido", c.entero(0), inicio.eliminarAcceso(usuario, c.entero(0)).nombre());

            // ---------------------------------------------------- RF-02
            case "LISINT":
                return listado("Información institucional", tablaSecciones(institucional.listar(c.parametro(0))));
            case "VERINT":
                return verSeccion(institucional.ver(c.parametro(0)));
            case "MODINT":
                institucional.modificar(usuario, c.parametro(0), c.parametro(1), c.parametro(2));
                return modificado("sección institucional", c.parametro(0));

            // ---------------------------------------------------- RF-16
            case "CONTACTO":
                return datosContacto();
            case "LISCFG":
                return listado("Datos configurables", tablaConfiguracion(contacto.listarConfiguracion(c.parametro(0))));
            case "MODCFG":
                contacto.modificarConfiguracion(usuario, c.parametro(0), c.parametro(1));
                return modificado("dato configurable", c.parametro(0));
            case "LISRED":
                return listado("Redes sociales", tablaRedes(contacto.listarRedes(c.parametro(0))));
            case "INSRED":
                return insertado("red social", contacto.insertarRed(usuario, red(0, c, 0)));
            case "MODRED":
                contacto.modificarRed(usuario, red(c.entero(0), c, 1));
                return modificado("red social", c.entero(0));
            case "ELIRED":
                return eliminado("red social", c.entero(0), contacto.eliminarRed(usuario, c.entero(0)).nombre());

            // ---------------------------------------------------- RF-19
            case "LISBIT":
                return listado("Bitácora", tablaBitacora(bitacora.listar(c.parametro(0))))
                        .agregar(new Parrafo("Nota", "Se muestran como máximo 200 registros, del más reciente al más antiguo."));
            default:
                throw new IllegalStateException("Comando sin implementar: " + c.nombre());
        }
    }

    // ================================================================ Armado de objetos desde los parámetros

    /** Los parámetros de un banner empiezan en la posición "desde" (0 en INSBAN, 1 en MODBAN). */
    private static Banner banner(int id, Comando c, int desde) {
        return new Banner(id, c.texto(desde), c.texto(desde + 1), c.texto(desde + 2), c.texto(desde + 3), c.entero(desde + 4));
    }

    private static Destacado destacado(int id, Comando c, int desde) {
        return new Destacado(id, c.texto(desde), c.texto(desde + 1), c.texto(desde + 2), c.entero(desde + 3));
    }

    private static AccesoRapido acceso(int id, Comando c, int desde) {
        return new AccesoRapido(id, c.texto(desde), c.texto(desde + 1), c.texto(desde + 2), c.entero(desde + 3));
    }

    private static RedSocial red(int id, Comando c, int desde) {
        return new RedSocial(id, c.texto(desde), c.texto(desde + 1), c.entero(desde + 2));
    }

    // ================================================================ Respuestas

    private Respuesta paginaInicio() throws SQLException, ErrorNegocio {
        InicioNegocio.PaginaInicio pagina = inicio.inicio();
        TablaHTML destacados = new TablaHTML("Contenidos destacados", "Título", "Descripción", "Enlace");
        for (Destacado d : pagina.destacados()) {
            destacados.fila(d.titulo(), d.descripcion(), d.enlace());
        }
        TablaHTML accesos = new TablaHTML("Accesos rápidos", "Nombre", "Enlace", "Descripción");
        for (AccesoRapido a : pagina.accesos()) {
            accesos.fila(a.nombre(), a.url(), a.descripcion());
        }
        TablaHTML banners = new TablaHTML("Banners", "Título", "Subtítulo", "Imagen", "Enlace");
        for (Banner b : pagina.banners()) {
            banners.fila(b.titulo(), b.subtitulo(), b.imagenUrl(), b.enlace());
        }
        return Respuesta.exito("Página de inicio", pagina.titulo())
                .agregar(new Parrafo("Bienvenida", pagina.bienvenida()))
                .agregar(banners)
                .agregar(destacados)
                .agregar(accesos);
    }

    private Respuesta verSeccion(SeccionInstitucional s) {
        return Respuesta.exito(s.titulo(), "Sección «" + s.clave() + "», actualizada el " + fecha(s.actualizado()) + ".")
                .agregar(new Parrafo(s.titulo(), s.contenido()));
    }

    private Respuesta datosContacto() throws SQLException, ErrorNegocio {
        ContactoNegocio.DatosContacto datos = contacto.contacto();
        TablaHTML tabla = new TablaHTML("Datos de contacto", "Dato", "Valor").sinCantidad();
        for (DatoConfiguracion d : datos.datos()) {
            tabla.fila(d.descripcion(), d.valor());
        }
        TablaHTML redes = new TablaHTML("Redes sociales", "Red", "Enlace");
        for (RedSocial r : datos.redes()) {
            redes.fila(r.nombre(), r.url());
        }
        return Respuesta.exito("Contacto", "Datos de contacto de la Carrera de Administración de Empresas.")
                .agregar(tabla)
                .agregar(redes);
    }

    private static TablaHTML tablaBanners(List<Banner> lista) {
        TablaHTML t = new TablaHTML("Banners", "ID", "Orden", "Título", "Subtítulo", "Imagen", "Enlace");
        for (Banner b : lista) {
            t.fila(b.id(), b.orden(), b.titulo(), b.subtitulo(), b.imagenUrl(), b.enlace());
        }
        return t;
    }

    private static TablaHTML tablaDestacados(List<Destacado> lista) {
        TablaHTML t = new TablaHTML("Contenidos destacados", "ID", "Orden", "Título", "Descripción", "Enlace");
        for (Destacado d : lista) {
            t.fila(d.id(), d.orden(), d.titulo(), d.descripcion(), d.enlace());
        }
        return t;
    }

    private static TablaHTML tablaAccesos(List<AccesoRapido> lista) {
        TablaHTML t = new TablaHTML("Accesos rápidos", "ID", "Orden", "Nombre", "Enlace", "Descripción");
        for (AccesoRapido a : lista) {
            t.fila(a.id(), a.orden(), a.nombre(), a.url(), a.descripcion());
        }
        return t;
    }

    private static TablaHTML tablaSecciones(List<SeccionInstitucional> lista) {
        TablaHTML t = new TablaHTML("Secciones institucionales", "Clave", "Título", "Actualizado");
        for (SeccionInstitucional s : lista) {
            t.fila(s.clave(), s.titulo(), fecha(s.actualizado()));
        }
        return t;
    }

    private static TablaHTML tablaConfiguracion(List<DatoConfiguracion> lista) {
        TablaHTML t = new TablaHTML("Datos configurables", "Clave", "Grupo", "Descripción", "Valor");
        for (DatoConfiguracion d : lista) {
            t.fila(d.clave(), d.grupo(), d.descripcion(), d.valor());
        }
        return t;
    }

    private static TablaHTML tablaRedes(List<RedSocial> lista) {
        TablaHTML t = new TablaHTML("Redes sociales", "ID", "Orden", "Red", "Enlace");
        for (RedSocial r : lista) {
            t.fila(r.id(), r.orden(), r.nombre(), r.url());
        }
        return t;
    }

    private static TablaHTML tablaBitacora(List<RegistroBitacora> lista) {
        TablaHTML t = new TablaHTML("Bitácora", "Fecha", "Usuario", "Acción", "Módulo", "Tabla", "Elemento", "Detalle");
        for (RegistroBitacora r : lista) {
            t.fila(fecha(r.fecha()), r.usuario(), r.accion(), r.modulo(), r.tabla(), r.elemento(), r.detalle());
        }
        return t;
    }
}
