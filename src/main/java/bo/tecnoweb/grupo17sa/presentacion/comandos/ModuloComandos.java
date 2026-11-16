package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.negocio.ErrorNegocio;
import bo.tecnoweb.grupo17sa.presentacion.lexico.Comando;
import bo.tecnoweb.grupo17sa.presentacion.vista.Respuesta;

import java.sql.SQLException;
import java.util.List;

/**
 * Comandos de un caso de uso. Cada integrante tiene su propia clase ComandosCUx,
 * así no se pisan al trabajar en paralelo; para activarla se agrega una línea en Despachador.
 */
public interface ModuloComandos {

    /** Código del caso de uso, por ejemplo "CU2". */
    String codigo();

    /** Nombre del caso de uso. */
    String nombre();

    /** Definición de cada comando (nombre, parámetros y tipos). */
    List<DefinicionComando> comandos();

    /**
     * Ejecuta un comando de este módulo. Cuando se llama, la cantidad y el tipo de los parámetros
     * ya se validaron y el remitente ya tiene permiso.
     */
    Respuesta ejecutar(Comando comando, String remitente) throws SQLException, ErrorNegocio;
}
