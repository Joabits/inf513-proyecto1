package bo.tecnoweb.grupo17sa.presentacion.vista;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Arma un archivo CSV que Excel abre bien en español: separador ";", UTF-8 con BOM y saltos CRLF.
 */
public class ArchivoCSV {

    private final String[] columnas;
    private final List<String[]> filas = new ArrayList<>();

    public ArchivoCSV(String... columnas) {
        this.columnas = columnas;
    }

    public ArchivoCSV fila(Object... celdas) {
        String[] fila = new String[columnas.length];
        for (int i = 0; i < columnas.length; i++) {
            fila[i] = i < celdas.length ? Objects.toString(celdas[i], "") : "";
        }
        filas.add(fila);
        return this;
    }

    public ArchivoAdjunto comoAdjunto(String nombreArchivo) {
        StringBuilder sb = new StringBuilder("﻿");   // BOM: Excel reconoce las tildes
        agregarLinea(sb, columnas);
        for (String[] fila : filas) {
            agregarLinea(sb, fila);
        }
        return new ArchivoAdjunto(nombreArchivo, "text/csv; charset=UTF-8", sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void agregarLinea(StringBuilder sb, String[] valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            String valor = valores[i];
            if (valor.contains(";") || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
                valor = "\"" + valor.replace("\"", "\"\"") + "\"";
            }
            sb.append(valor);
        }
        sb.append("\r\n");
    }
}
