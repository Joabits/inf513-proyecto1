package bo.tecnoweb.grupo17sa.modelo;

/** Resultado de un GROUP BY: un valor (por ejemplo, un sector) y cuántos registros tiene. */
public record Conteo(String etiqueta, long cantidad) {
}
