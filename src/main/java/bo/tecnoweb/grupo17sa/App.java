package bo.tecnoweb.grupo17sa;

import bo.tecnoweb.grupo17sa.datos.ConexionDB;
import bo.tecnoweb.grupo17sa.datos.InstaladorBD;
import bo.tecnoweb.grupo17sa.presentacion.comandos.Despachador;
import bo.tecnoweb.grupo17sa.presentacion.correo.EmisorCorreo;
import bo.tecnoweb.grupo17sa.presentacion.correo.HiloCiclico;
import bo.tecnoweb.grupo17sa.presentacion.correo.LectorCorreo;
import bo.tecnoweb.grupo17sa.presentacion.vista.ArchivoAdjunto;
import bo.tecnoweb.grupo17sa.presentacion.vista.PlantillaHTML;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sistema vía e-mail del Portal de la Carrera de Administración de Empresas (Grupo 17 SA).
 *
 * Uso:
 *   java -jar portal-correo.jar                    inicia el servicio: revisa el buzón cada N segundos
 *   java -jar portal-correo.jar --una-vez          revisa el buzón una sola vez y termina
 *   java -jar portal-correo.jar --instalar-bd      crea las tablas y carga los datos iniciales
 *   java -jar portal-correo.jar --probar "INICIO" [--de correo@x.com]   ejecuta un comando sin correo
 *   java -jar portal-correo.jar --consola [--de correo@x.com]           escribe comandos uno por uno
 *   java -jar portal-correo.jar --probar-correo    prueba la conexión POP3 y SMTP sin enviar nada
 *   java -jar portal-correo.jar --enviar "INICIO" --a correo@x.com [--de correo@y.com]   envía la respuesta por SMTP
 *   Opción común: --config ruta/config.properties
 */
public class App {

    /** Remitente que se usa en --probar y --consola si no se indica --de. */
    private static final String VISITANTE = "visitante@prueba.local";
    private static final Set<String> OPCIONES_CON_VALOR = Set.of("--config", "--probar", "--de", "--enviar", "--a");

    public static void main(String[] args) {
        Map<String, String> opciones = leerOpciones(args);
        String rutaConfig = opciones.getOrDefault("--config", "config.properties");
        try {
            Configuracion.cargar(rutaConfig);
        } catch (IOException e) {
            System.out.println("No se pudo leer " + rutaConfig + ": " + e.getMessage());
            System.out.println("Copie config.properties.ejemplo como config.properties y complete sus datos.");
            System.exit(1);
        }
        String remitente = opciones.getOrDefault("--de", VISITANTE);
        try {
            if (opciones.containsKey("--instalar-bd")) {
                InstaladorBD.instalar();
            } else if (opciones.containsKey("--probar")) {
                mostrar(new Despachador().procesar(remitente, opciones.get("--probar")), opciones.get("--probar"));
            } else if (opciones.containsKey("--consola")) {
                consola(remitente);
            } else if (opciones.containsKey("--probar-correo")) {
                probarCorreo();
            } else if (opciones.containsKey("--enviar")) {
                String destino = opciones.getOrDefault("--a", "");
                enviar(opciones.get("--enviar"), destino, opciones.getOrDefault("--de", destino));
            } else {
                servicio(opciones.containsKey("--una-vez"));
            }
        } catch (Exception e) {
            Registro.error("El programa terminó con error", e);
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void servicio(boolean unaVez) throws Exception {
        System.out.println("=== Portal de Administración de Empresas - Sistema vía E-Mail (Grupo 17 SA) ===");
        System.out.println("Base de datos: " + ConexionDB.probar());
        HiloCiclico hilo = new HiloCiclico();
        if (unaVez) {
            hilo.revisarBuzon();
            return;
        }
        hilo.start();
        System.out.println("Servicio iniciado: revisa el buzón cada " + hilo.intervaloSegundos()
                + " segundos (Ctrl+C para detener).");
        hilo.join();
    }

    private static void consola(String remitente) throws IOException {
        System.out.println("Consola de prueba (remitente " + remitente + "). Escriba un comando; línea vacía para salir.");
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        Despachador despachador = new Despachador();
        while (true) {
            System.out.print("> ");
            String linea = teclado.readLine();
            if (linea == null || linea.isBlank()) {
                return;
            }
            mostrar(despachador.procesar(remitente, linea), linea);
        }
    }

    /** Muestra la respuesta en la consola y guarda el correo HTML en salida/ para verlo en el navegador. */
    private static void mostrar(Respuesta respuesta, String asunto) throws IOException {
        System.out.println("Asunto de la respuesta: " + PlantillaHTML.asunto(respuesta));
        System.out.println(PlantillaHTML.texto(respuesta));
        Path carpeta = Path.of("salida");
        Files.createDirectories(carpeta);
        String nombre = respuesta.comando().isEmpty() ? "respuesta" : respuesta.comando();
        Path archivo = carpeta.resolve(nombre + ".html");
        Files.writeString(archivo, PlantillaHTML.html(respuesta, asunto), StandardCharsets.UTF_8);
        System.out.println("Vista del correo: " + archivo.toAbsolutePath());
        for (ArchivoAdjunto adjunto : respuesta.adjuntos()) {
            Path destino = carpeta.resolve(adjunto.nombre());
            Files.write(destino, adjunto.contenido());
            System.out.println("Adjunto guardado: " + destino.toAbsolutePath());
        }
        int numero = 0;
        for (Respuesta.Aviso aviso : respuesta.avisos()) {
            Path destino = carpeta.resolve("AVISO-" + nombre + (++numero) + ".html");
            Files.writeString(destino, PlantillaHTML.html(aviso.respuesta(), asunto), StandardCharsets.UTF_8);
            System.out.println("Aviso para " + aviso.destinatario() + " (" + PlantillaHTML.asunto(aviso.respuesta())
                    + "): " + destino.toAbsolutePath());
        }
        System.out.println();
    }

    /** Ejecuta un comando como si lo enviara "remitente" y manda la respuesta por SMTP a "destino" (sin leer el buzón). */
    private static void enviar(String asunto, String destino, String remitente) throws IOException {
        if (destino.isBlank()) {
            throw new IOException("Falta el destinatario: --enviar \"COMANDO\" --a correo@ejemplo.com");
        }
        Respuesta respuesta = new Despachador().procesar(remitente, asunto);
        new EmisorCorreo().enviar(destino, PlantillaHTML.asunto(respuesta), PlantillaHTML.html(respuesta, asunto), null,
                respuesta.adjuntos());
        Registro.acceso(destino + " | respuesta enviada (--enviar) | " + PlantillaHTML.asunto(respuesta));
        System.out.println("Enviado a " + destino + ": " + PlantillaHTML.asunto(respuesta));
    }

    private static void probarCorreo() {
        try {
            System.out.println("POP3: " + new LectorCorreo().probarConexion());
        } catch (IOException e) {
            System.out.println("POP3: Error - " + e.getMessage());
        }
        try {
            System.out.println("SMTP: " + new EmisorCorreo().probarConexion());
        } catch (IOException e) {
            System.out.println("SMTP: Error - " + e.getMessage());
        }
    }

    private static Map<String, String> leerOpciones(String[] args) {
        Map<String, String> opciones = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (OPCIONES_CON_VALOR.contains(args[i]) && i + 1 < args.length) {
                opciones.put(args[i], args[++i]);
            } else {
                opciones.put(args[i], "");
            }
        }
        return opciones;
    }
}
