package bo.tecnoweb.grupo17sa.modelo;

/** Edición de la revista INNOVAR (tabla revista_edicion, RF-08); articulos es la cantidad de artículos. */
public record EdicionRevista(int id, Integer volumen, int numero, int anio, String titulo, String descripcion,
                             String enlacePdf, boolean publicada, int articulos) {

    /** «Volumen 2 · Número 7 · 2026» (sin volumen: «Número 7 · 2026»). */
    public String nombre() {
        return (volumen == null ? "" : "Volumen " + volumen + " · ") + "Número " + numero + " · " + anio;
    }
}
