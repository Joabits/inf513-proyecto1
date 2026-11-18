package bo.tecnoweb.grupo17sa.modelo;

import java.util.List;

/** Rol del sistema y los casos de uso que administra (tablas rol y rol_modulo, RF-18). */
public record Rol(int id, String nombre, String descripcion, List<String> modulos, int usuarios) {
}
