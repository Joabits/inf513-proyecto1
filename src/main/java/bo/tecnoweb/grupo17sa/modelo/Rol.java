package bo.tecnoweb.grupo17sa.modelo;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Roles fijos del sistema (RF-18). No se crean ni se modifican por correo: cada rol gestiona siempre lo mismo, y esta
 * clase es la única que lo define. Quien no tiene usuario (o lo tiene inactivo) es Visitante y solo usa los comandos
 * públicos; los procesos automáticos (avisos por correo) los hace el Sistema, que tampoco es un rol de la base.
 */
public enum Rol {

    ADMINISTRADOR("Administrador",
            "Director de Carrera. Gestiona todo: usuarios, páginas institucionales (historia, misión, visión), datos de"
                    + " contacto, redes sociales, contenidos destacados, bitácora y reportes, además de lo de los editores.",
            true, Set.of(), Set.of()),

    EDITOR_INSTITUCIONAL("Editor institucional",
            "Oferta académica, malla, titulación, autoridades y personal (CU3); investigación, documentos descargables y"
                    + " revista INNOVAR (CU4); vinculación, INNOVA y Alumni (CU5); mensajes de contacto (CU7) y"
                    + " enlaces de acceso rápido (CU2).",
            false, Set.of("CU3", "CU4", "CU5", "CU7"), Set.of("LISACC", "INSACC", "MODACC", "ELIACC")),

    EDITOR_COMUNICACION("Editor de comunicación",
            "Noticias, comunicados, eventos y galería (CU6) y banners del carrusel de inicio (CU2).",
            false, Set.of("CU6"), Set.of("LISBAN", "INSBAN", "MODBAN", "ELIBAN"));

    private final String nombre;
    private final String gestiona;
    private final boolean todo;
    private final Set<String> modulos;
    private final Set<String> comandos;

    Rol(String nombre, String gestiona, boolean todo, Set<String> modulos, Set<String> comandos) {
        this.nombre = nombre;
        this.gestiona = gestiona;
        this.todo = todo;
        this.modulos = modulos;
        this.comandos = comandos;
    }

    /** Nombre para mostrar, por ejemplo «Editor de comunicación». */
    public String nombre() {
        return nombre;
    }

    /** Qué gestiona el rol, en una frase. */
    public String gestiona() {
        return gestiona;
    }

    /** Comandos sueltos de otros casos de uso que el rol puede usar (por ejemplo los banners del CU2). */
    public Set<String> comandos() {
        return comandos;
    }

    /** ¿El rol puede usar el comando privado indicado del caso de uso (módulo)? */
    public boolean puede(String modulo, String comando) {
        return todo || modulos.contains(modulo) || comandos.contains(comando);
    }

    /** Roles que pueden usar un comando privado, en orden (para HELP, el permiso denegado y los avisos por correo). */
    public static List<Rol> quePueden(String modulo, String comando) {
        List<Rol> roles = new ArrayList<>();
        for (Rol rol : values()) {
            if (rol.puede(modulo, comando)) {
                roles.add(rol);
            }
        }
        return roles;
    }

    /** «Administrador y Editor de comunicación». */
    public static String nombres(List<Rol> roles) {
        List<String> nombres = new ArrayList<>();
        for (Rol rol : roles) {
            nombres.add(rol.nombre());
        }
        if (nombres.size() < 2) {
            return String.join("", nombres);
        }
        return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.get(nombres.size() - 1);
    }

    /**
     * Lee el rol que escribió el administrador: «EDITOR_COMUNICACION», «editor de comunicación» y «Editor comunicacion»
     * son el mismo rol. Devuelve null si no es ninguno de los tres.
     */
    public static Rol leer(String texto) {
        if (texto == null) {
            return null;
        }
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String clave = ("_" + sinTildes.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_") + "_")
                .replace("_DE_", "_").replaceAll("^_+|_+$", "");
        for (Rol rol : values()) {
            if (rol.name().equals(clave)) {
                return rol;
            }
        }
        return null;
    }
}
