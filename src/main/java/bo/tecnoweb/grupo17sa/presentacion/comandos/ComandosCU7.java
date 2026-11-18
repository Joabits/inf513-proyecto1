package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.modelo.MensajeContacto;
import bo.tecnoweb.grupo17sa.modelo.ResultadoBusqueda;
import bo.tecnoweb.grupo17sa.negocio.BusquedaNegocio;
import bo.tecnoweb.grupo17sa.negocio.ErrorNegocio;
import bo.tecnoweb.grupo17sa.negocio.MensajesNegocio;
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
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.eliminado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.fecha;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.insertado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.listado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.modificado;

/** CU7 Formulario de Contacto y Búsqueda Interna Global (RF-15 y RF-17). */
public class ComandosCU7 implements ModuloComandos {

    private static final List<DefinicionComando> COMANDOS = List.of(
            publico("ENVMSJ", "Envía un mensaje a la Carrera; la respuesta llega a su correo (teléfono opcional: \"\")",
                    texto("nombre", 150), textoOpcional("telefono", 30), texto("asunto", 150), texto("mensaje", 1000)),
            publico("MISMSJ", "Muestra el estado y la respuesta de sus mensajes"),
            privado("LISMSJ", "Lista los mensajes: \"*\" (todos), un estado (NUEVO, LEIDO, RESPONDIDO, ARCHIVADO) o un texto",
                    filtro()),
            privado("VERMSJ", "Muestra un mensaje completo y lo marca como leído", entero("id")),
            privado("RESMSJ", "Responde un mensaje: la respuesta se envía por correo al remitente", entero("id"),
                    texto("respuesta", 1000)),
            privado("ARCMSJ", "Archiva un mensaje ya atendido", entero("id")),
            privado("ELIMSJ", "Elimina un mensaje", entero("id")),
            publico("BUSCAR", "Busca en todo el contenido publicado del portal, sin distinguir tildes (3 a 80 caracteres)",
                    texto("texto", 80))
    );

    private final MensajesNegocio mensajes = new MensajesNegocio();
    private final BusquedaNegocio busqueda = new BusquedaNegocio();

    @Override
    public String codigo() {
        return MensajesNegocio.MODULO;
    }

    @Override
    public String nombre() {
        return "Formulario de Contacto y Búsqueda Interna Global";
    }

    @Override
    public List<DefinicionComando> comandos() {
        return COMANDOS;
    }

    @Override
    public Respuesta ejecutar(Comando c, String usuario) throws SQLException, ErrorNegocio {
        switch (c.nombre()) {
            case "ENVMSJ":
                return enviar(c, usuario);
            case "MISMSJ":
                return listado("Mis mensajes", tablaMisMensajes(mensajes.misMensajes(usuario)));
            case "LISMSJ":
                return listado("Mensajes de contacto", tablaMensajes(mensajes.listar(c.parametro(0))));
            case "VERMSJ":
                return verMensaje(mensajes.ver(usuario, c.entero(0)));
            case "RESMSJ":
                return responder(c, usuario);
            case "ARCMSJ":
                mensajes.archivar(usuario, c.entero(0));
                return modificado("mensaje", c.entero(0));
            case "ELIMSJ":
                return eliminado("mensaje", c.entero(0), mensajes.eliminar(usuario, c.entero(0)).asunto());
            case "BUSCAR":
                return buscar(c.parametro(0));
            default:
                throw new IllegalStateException("Comando sin implementar: " + c.nombre());
        }
    }

    private Respuesta enviar(Comando c, String correo) throws SQLException, ErrorNegocio {
        MensajesNegocio.Envio envio = mensajes.enviar(correo, c.parametro(0), c.texto(1), c.parametro(2), c.parametro(3));
        Respuesta respuesta = insertado("mensaje de contacto", envio.id())
                .agregar(new Parrafo("Siguiente paso", "La Carrera responderá a este correo. Mientras tanto puede ver"
                        + " el estado con MISMSJ."));
        Respuesta aviso = Respuesta.aviso("Nuevo mensaje de contacto N° " + envio.id(),
                        "Llegó un mensaje de " + c.parametro(0) + " (" + correo + "). Respóndalo con RESMSJ[\"" + envio.id()
                                + "\",\"respuesta\"].")
                .agregar(new Parrafo(c.parametro(2), c.parametro(3)));
        for (String destinatario : envio.avisar()) {
            if (!destinatario.equalsIgnoreCase(correo)) {
                respuesta.avisar(destinatario, aviso);
            }
        }
        return respuesta;
    }

    private Respuesta responder(Comando c, String usuario) throws SQLException, ErrorNegocio {
        int id = c.entero(0);
        String texto = c.parametro(1).trim();
        MensajeContacto m = mensajes.responder(usuario, id, texto);
        Respuesta aviso = Respuesta.aviso("Respuesta de la Carrera a su mensaje: " + m.asunto(),
                        "La Carrera de Administración de Empresas respondió su mensaje N° " + id + ".")
                .agregar(new Parrafo("Respuesta", texto))
                .agregar(new Parrafo("Su mensaje original", m.mensaje()));
        return Respuesta.exito("Mensaje respondido", "Registro modificado correctamente (mensaje " + id + " de "
                        + m.nombre() + " marcado como RESPONDIDO).")
                .agregar(new Parrafo("Aviso", "Se envía la respuesta por correo a " + m.correo() + "."))
                .avisar(m.correo(), aviso);
    }

    private Respuesta buscar(String texto) throws SQLException, ErrorNegocio {
        List<ResultadoBusqueda> resultados = busqueda.buscar(texto);
        int total = resultados.stream().mapToInt(ResultadoBusqueda::total).sum();
        if (total == 0) {
            return Respuesta.exito("Búsqueda", "No se encontraron resultados para «" + texto.trim() + "».")
                    .agregar(new Parrafo("Sugerencia", "Pruebe con otra palabra o con menos palabras."));
        }
        Respuesta respuesta = Respuesta.exito("Búsqueda", total + (total == 1 ? " resultado" : " resultados")
                + " para «" + texto.trim() + "».");
        for (ResultadoBusqueda r : resultados) {
            TablaHTML t = new TablaHTML(r.seccion() + " (" + r.total() + ")", "Resultado", "Detalle").sinCantidad();
            for (ResultadoBusqueda.Coincidencia co : r.coincidencias()) {
                t.fila(co.titulo(), co.detalle());
            }
            if (r.total() > r.coincidencias().size()) {
                t.fila("… y " + (r.total() - r.coincidencias().size()) + " más", "Afine la búsqueda para verlos");
            }
            respuesta.agregar(t);
        }
        return respuesta;
    }

    private static Respuesta verMensaje(MensajeContacto m) {
        TablaHTML t = new TablaHTML("Mensaje N° " + m.id(), "Dato", "Valor").sinCantidad()
                .fila("Fecha", fecha(m.fecha()))
                .fila("Nombre", m.nombre())
                .fila("Correo", m.correo())
                .fila("Teléfono", m.telefono() == null ? "" : m.telefono())
                .fila("Asunto", m.asunto())
                .fila("Estado", m.estado());
        Respuesta respuesta = Respuesta.exito("Mensaje de contacto", "Mensaje " + m.id() + " de " + m.nombre() + ".")
                .agregar(t).agregar(new Parrafo("Mensaje", m.mensaje()));
        if (m.respuesta() != null) {
            respuesta.agregar(new Parrafo("Respuesta de " + m.respondidoPor() + " (" + fecha(m.respondidoEn()) + ")", m.respuesta()));
        }
        return respuesta;
    }

    private static TablaHTML tablaMensajes(List<MensajeContacto> lista) {
        TablaHTML t = new TablaHTML("Mensajes de contacto", "ID", "Fecha", "Nombre", "Correo", "Asunto", "Estado");
        for (MensajeContacto m : lista) {
            t.fila(m.id(), fecha(m.fecha()), m.nombre(), m.correo(), m.asunto(), m.estado());
        }
        return t;
    }

    private static TablaHTML tablaMisMensajes(List<MensajeContacto> lista) {
        TablaHTML t = new TablaHTML("Mis mensajes", "ID", "Fecha", "Asunto", "Estado", "Respuesta");
        for (MensajeContacto m : lista) {
            t.fila(m.id(), fecha(m.fecha()), m.asunto(), m.estado(), m.respuesta() == null ? "" : m.respuesta());
        }
        return t;
    }
}
