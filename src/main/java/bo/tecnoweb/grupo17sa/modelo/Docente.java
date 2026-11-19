package bo.tecnoweb.grupo17sa.modelo;

/** Docente de la carrera y su materia asignada (tabla docente, CU3). */
public record Docente(int id, String nombre, String titulo, String cargo, String correo, Integer materiaId, String materiaNombre) {

    public Docente(int id, String nombre, String titulo, String cargo, String correo, Integer materiaId) {
        this(id, nombre, titulo, cargo, correo, materiaId, null);
    }
}
