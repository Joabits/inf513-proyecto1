package bo.tecnoweb.grupo17sa.modelo;

/** Materia de la malla curricular (tabla materia, CU3). */
public record Materia(int id, String sigla, String nombre, int semestre, int creditos, String prerrequisito) {
}
