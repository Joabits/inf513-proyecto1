package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.Registro;
import bo.tecnoweb.grupo17sa.negocio.AccesoNegocio;
import bo.tecnoweb.grupo17sa.negocio.ErrorNegocio;
import bo.tecnoweb.grupo17sa.presentacion.lexico.AnalizadorLexico;
import bo.tecnoweb.grupo17sa.presentacion.lexico.Comando;
import bo.tecnoweb.grupo17sa.presentacion.lexico.ErrorSintaxis;
import bo.tecnoweb.grupo17sa.presentacion.vista.Parrafo;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;
import bo.tecnoweb.grupo17sa.presentacion.vista.TablaHTML;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recibe el asunto de un correo y devuelve la respuesta. Pasos:
 *  1. Análisis léxico del asunto (AnalizadorLexico).
 *  2. Busca el comando en los módulos de cada caso de uso.
 *  3. Valida la cantidad exacta de parámetros y el tipo de cada uno.
 *  4. Verifica el permiso del remitente si el comando no es público.
 *  5. Ejecuta el comando; los errores de negocio y de PostgreSQL se responden como "Error: ...".
 * Cada correo procesado queda en logs/access.log y cada excepción en logs/error.log.
 */
public class Despachador {

    private final AnalizadorLexico analizador = new AnalizadorLexico();
    private final AccesoNegocio acceso = new AccesoNegocio();

    /** Un módulo por caso de uso. Para activar un caso de uso nuevo, agregue aquí su clase. */
    private final List<ModuloComandos> modulos = List.of(
            new ComandosCU1(),
            new ComandosCU2(),
            new ComandosCU5(),
            new ComandosCU7(),
            new ComandosCU8()
    );

    public Despachador() {
        // Dos casos de uso no pueden usar el mismo nombre de comando: se avisa al iniciar, no en la defensa.
        Map<String, String> usados = new HashMap<>();
        for (ModuloComandos modulo : modulos) {
            for (DefinicionComando definicion : modulo.comandos()) {
                String anterior = usados.put(definicion.nombre(), modulo.codigo());
                if (anterior != null || definicion.nombre().equals("HELP")) {
                    throw new IllegalStateException("El comando " + definicion.nombre() + " está repetido en "
                            + (anterior == null ? "HELP" : anterior) + " y " + modulo.codigo() + ".");
                }
            }
        }
    }

    public Respuesta procesar(String remitente, String asunto) {
        long inicio = System.currentTimeMillis();
        String correo = remitente == null ? "" : remitente.trim().toLowerCase(Locale.ROOT);
        Respuesta respuesta = ejecutar(correo, asunto);
        Registro.acceso(correo + " | " + asunto + " | " + (respuesta.esExito() ? "ÉXITO" : "ERROR")
                + " | " + respuesta.titulo() + (respuesta.esExito() ? "" : ": " + respuesta.mensaje())
                + " | " + (System.currentTimeMillis() - inicio) + " ms");
        return respuesta;
    }

    private Respuesta ejecutar(String remitente, String asunto) {
        Comando comando;
        try {
            comando = analizador.analizar(asunto);
        } catch (ErrorSintaxis e) {
            return Respuesta.error("Comando mal escrito", e.getMessage())
                    .agregar(new Parrafo("Formato correcto", "COMANDO[\"parámetro 1\",\"parámetro 2\",...]\n"
                            + "Envíe HELP para ver la lista de comandos."));
        }
        return ejecutarComando(remitente, comando).conComando(comando.nombre());
    }

    private Respuesta ejecutarComando(String remitente, Comando comando) {
        if (comando.nombre().equals("HELP")) {
            return ayuda(comando);
        }
        for (ModuloComandos modulo : modulos) {
            DefinicionComando definicion = buscar(modulo, comando.nombre());
            if (definicion == null) {
                continue;
            }
            String problema = validarParametros(definicion, comando);
            if (problema != null) {
                return Respuesta.error("Parámetros incorrectos", problema)
                        .agregar(new Parrafo("Sintaxis de " + definicion.nombre(),
                                definicion.sintaxis() + "\n" + definicion.descripcionCompleta()));
            }
            if (!definicion.esPublico() && !acceso.tienePermiso(remitente, modulo.codigo(), definicion.nombre())) {
                return Respuesta.error("Permiso denegado", "El correo " + remitente + " no tiene permiso para usar "
                        + definicion.nombre() + " (" + modulo.codigo() + " " + modulo.nombre() + ").");
            }
            try {
                return modulo.ejecutar(comando, remitente);
            } catch (ErrorNegocio e) {
                return Respuesta.error("No se pudo completar " + definicion.nombre(), e.getMessage());
            } catch (SQLException e) {
                Registro.error("Error SQL en " + comando + " (remitente " + remitente + ")", e);
                return Respuesta.error("Error en la base de datos", ErroresSQL.describir(e));
            } catch (RuntimeException e) {
                Registro.error("Error inesperado en " + comando + " (remitente " + remitente + ")", e);
                return Respuesta.error("Error interno", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        return Respuesta.error("Comando desconocido", "El comando " + comando.nombre()
                + " no existe. Envíe HELP para ver la lista de comandos.");
    }

    private static DefinicionComando buscar(ModuloComandos modulo, String nombre) {
        for (DefinicionComando definicion : modulo.comandos()) {
            if (definicion.nombre().equals(nombre)) {
                return definicion;
            }
        }
        return null;
    }

    /** Cantidad exacta de parámetros y tipo de cada uno; devuelve null si todo está bien. */
    private static String validarParametros(DefinicionComando definicion, Comando comando) {
        List<Parametro> esperados = definicion.parametros();
        if (comando.cantidad() != esperados.size()) {
            return definicion.nombre() + " requiere " + esperados.size() + " parámetro(s) y se recibieron "
                    + comando.cantidad() + ".";
        }
        for (int i = 0; i < esperados.size(); i++) {
            String motivo = esperados.get(i).validar(comando.parametro(i));
            if (motivo != null) {
                return "El parámetro " + (i + 1) + " (" + esperados.get(i).nombre() + ") " + motivo
                        + ". Valor recibido: \"" + comando.parametro(i) + "\".";
            }
        }
        return null;
    }

    /** HELP lista todos los comandos; HELP["CU2"] solo los de un caso de uso. */
    private Respuesta ayuda(Comando comando) {
        if (comando.cantidad() > 1) {
            return Respuesta.error("Parámetros incorrectos", "HELP se usa sin parámetros o con uno: HELP[\"CU2\"].");
        }
        String filtro = comando.cantidad() == 1 ? comando.parametro(0).toUpperCase(Locale.ROOT) : "";
        Respuesta respuesta = Respuesta.exito("Ayuda", "Comandos disponibles. Formato: COMANDO[\"parámetro 1\",\"parámetro 2\",...]. "
                + "Los parámetros opcionales se envían vacíos (\"\"); dentro de un parámetro, \\\" escribe una comilla.");
        for (ModuloComandos modulo : modulos) {
            if (!filtro.isEmpty() && !modulo.codigo().equals(filtro)) {
                continue;
            }
            TablaHTML tabla = new TablaHTML(modulo.codigo() + " - " + modulo.nombre(), "Comando", "Descripción", "Acceso");
            for (DefinicionComando d : modulo.comandos()) {
                tabla.fila(d.sintaxis(), d.descripcionCompleta(), d.esPublico() ? "Público" : "Con permiso");
            }
            respuesta.agregar(tabla);
        }
        if (respuesta.bloques().isEmpty()) {
            return Respuesta.error("Módulo desconocido", "No existe el módulo " + filtro + ". Envíe HELP para ver todos.");
        }
        return respuesta;
    }
}
