package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.modelo.Docente;
import bo.tecnoweb.grupo17sa.modelo.InfoAcademica;
import bo.tecnoweb.grupo17sa.modelo.Materia;
import bo.tecnoweb.grupo17sa.negocio.AcademicaNegocio;
import bo.tecnoweb.grupo17sa.negocio.ErrorNegocio;
import bo.tecnoweb.grupo17sa.presentacion.lexico.Comando;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import bo.tecnoweb.grupo17sa.presentacion.vista.TablaHTML;

import java.sql.SQLException;
import java.util.List;

import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.privado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.DefinicionComando.publico;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.correoOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.entero;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.enteroOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.filtro;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.texto;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Parametro.textoOpcional;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.eliminado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.fecha;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.insertado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.listado;
import static bo.tecnoweb.grupo17sa.presentacion.comandos.Respuestas.modificado;

/**
 * CU3 Información Académica, Malla Interactiva y Plantel Docente (RF-03, RF-04, RF-05, RF-06).
 */
public class ComandosCU3 implements ModuloComandos {

    private static final Parametro[] INFO = {
            texto("titulo", 150),
            texto("contenido", 5000),
            entero("orden")
    };

    private static final Parametro[] MATERIA = {
            texto("sigla", 20),
            texto("nombre", 150),
            entero("semestre"),
            entero("creditos"),
            textoOpcional("prerrequisito", 150)
    };

    private static final Parametro[] DOCENTE = {
            texto("nombre", 150),
            textoOpcional("titulo", 150),
            textoOpcional("cargo", 100),
            correoOpcional("correo", 120),
            enteroOpcional("materia_id")
    };

    private static final List<DefinicionComando> COMANDOS = List.of(
            // Información Académica (RF-03, RF-04)
            publico("LISINF", "Lista la información académica y reglamentos (filtre con \"*\" o texto)", filtro()),
            privado("INSINF", "Registra una sección de información académica", INFO),
            privado("MODINF", "Modifica una sección de información académica", conId(INFO)),
            privado("ELIINF", "Elimina una sección de información académica", entero("id")),

            // Malla Curricular / Materias (RF-05)
            publico("LISMAT", "Lista las materias de la malla curricular (filtre por sigla, nombre o prerrequisito)", filtro()),
            privado("INSMAT", "Registra una materia en la malla curricular", MATERIA),
            privado("MODMAT", "Modifica una materia de la malla curricular", conId(MATERIA)),
            privado("ELIMAT", "Elimina una materia de la malla curricular", entero("id")),

            // Plantel Docente (RF-06)
            publico("LISDOC", "Lista el plantel docente (filtre por nombre, título, cargo, correo o materia)", filtro()),
            privado("INSDOC", "Registra un docente en el plantel", DOCENTE),
            privado("MODDOC", "Modifica un docente del plantel", conId(DOCENTE)),
            privado("ELIDOC", "Elimina un docente del plantel", entero("id"))
    );

    private final AcademicaNegocio academica = new AcademicaNegocio();

    @Override
    public String codigo() {
        return "CU3";
    }

    @Override
    public String nombre() {
        return "Información Académica, Malla Curricular y Plantel Docente";
    }

    @Override
    public List<DefinicionComando> comandos() {
        return COMANDOS;
    }

    @Override
    public Respuesta ejecutar(Comando c, String usuario) throws SQLException, ErrorNegocio {
        switch (c.nombre()) {
            case "LISINF":
                return listado("Información académica", tablaInfo(academica.listarInfo(c.parametro(0))));
            case "INSINF":
                return insertado("información académica", academica.insertarInfo(usuario, info(0, c, 0)));
            case "MODINF":
                academica.modificarInfo(usuario, info(c.entero(0), c, 1));
                return modificado("información académica", c.entero(0));
            case "ELIINF":
                return eliminado("información académica", c.entero(0), academica.eliminarInfo(usuario, c.entero(0)).titulo());

            case "LISMAT":
                return listado("Malla curricular - Materias", tablaMaterias(academica.listarMaterias(c.parametro(0))));
            case "INSMAT":
                return insertado("materia", academica.insertarMateria(usuario, materia(0, c, 0)));
            case "MODMAT":
                academica.modificarMateria(usuario, materia(c.entero(0), c, 1));
                return modificado("materia", c.entero(0));
            case "ELIMAT":
                Materia borrada = academica.eliminarMateria(usuario, c.entero(0));
                return eliminado("materia", c.entero(0), borrada.sigla() + " - " + borrada.nombre());

            case "LISDOC":
                return listado("Plantel docente", tablaDocentes(academica.listarDocentes(c.parametro(0))));
            case "INSDOC":
                return insertado("docente", academica.insertarDocente(usuario, docente(0, c, 0)));
            case "MODDOC":
                academica.modificarDocente(usuario, docente(c.entero(0), c, 1));
                return modificado("docente", c.entero(0));
            case "ELIDOC":
                return eliminado("docente", c.entero(0), academica.eliminarDocente(usuario, c.entero(0)).nombre());

            default:
                throw new IllegalStateException("Comando sin implementar: " + c.nombre());
        }
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

    private static InfoAcademica info(int id, Comando c, int offset) {
        return new InfoAcademica(id, c.texto(offset), c.texto(offset + 1), c.entero(offset + 2));
    }

    private static Materia materia(int id, Comando c, int offset) {
        return new Materia(id, c.texto(offset), c.texto(offset + 1), c.entero(offset + 2), c.entero(offset + 3), c.texto(offset + 4));
    }

    private static Docente docente(int id, Comando c, int offset) {
        return new Docente(id, c.texto(offset), c.texto(offset + 1), c.texto(offset + 2), c.texto(offset + 3), c.enteroOpcional(offset + 4));
    }

    // =========================================================================
    // Tablas HTML para respuestas
    // =========================================================================

    private static TablaHTML tablaInfo(List<InfoAcademica> lista) {
        TablaHTML t = new TablaHTML("Información académica", "ID", "Título", "Contenido", "Orden", "Actualizado");
        for (InfoAcademica i : lista) {
            t.fila(i.id(), i.titulo(), i.contenido(), i.orden(), fecha(i.actualizado()));
        }
        return t;
    }

    private static TablaHTML tablaMaterias(List<Materia> lista) {
        TablaHTML t = new TablaHTML("Malla curricular", "ID", "Sigla", "Materia", "Semestre", "Créditos", "Prerrequisito");
        for (Materia m : lista) {
            t.fila(m.id(), m.sigla(), m.nombre(), m.semestre(), m.creditos(),
                    m.prerrequisito() == null ? "Ninguno" : m.prerrequisito());
        }
        return t;
    }

    private static TablaHTML tablaDocentes(List<Docente> lista) {
        TablaHTML t = new TablaHTML("Plantel docente", "ID", "Nombre", "Título", "Cargo", "Correo", "Materia Asignada");
        for (Docente d : lista) {
            t.fila(d.id(), d.nombre(),
                    d.titulo() == null ? "" : d.titulo(),
                    d.cargo() == null ? "" : d.cargo(),
                    d.correo() == null ? "" : d.correo(),
                    d.materiaNombre() == null ? "(Sin materia)" : d.materiaNombre());
        }
        return t;
    }
}
