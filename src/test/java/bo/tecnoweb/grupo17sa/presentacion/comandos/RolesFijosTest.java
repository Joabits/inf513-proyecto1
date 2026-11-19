package bo.tecnoweb.grupo17sa.presentacion.comandos;

import bo.tecnoweb.grupo17sa.modelo.Rol;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static bo.tecnoweb.grupo17sa.modelo.Rol.ADMINISTRADOR;
import static bo.tecnoweb.grupo17sa.modelo.Rol.EDITOR_COMUNICACION;
import static bo.tecnoweb.grupo17sa.modelo.Rol.EDITOR_INSTITUCIONAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de permisos de los tres roles fijos, comparada con los comandos que existen de verdad. */
class RolesFijosTest {

    private static final List<ModuloComandos> MODULOS = List.of(new ComandosCU1(), new ComandosCU2(), new ComandosCU3(),
            new ComandosCU4(), new ComandosCU5(), new ComandosCU6(), new ComandosCU7(), new ComandosCU8());

    /** Comando privado → caso de uso al que pertenece. */
    private static Map<String, String> privados() {
        Map<String, String> privados = new HashMap<>();
        for (ModuloComandos modulo : MODULOS) {
            for (DefinicionComando d : modulo.comandos()) {
                if (!d.esPublico()) {
                    privados.put(d.nombre(), modulo.codigo());
                }
            }
        }
        return privados;
    }

    private static boolean puede(Rol rol, String comando) {
        String modulo = privados().get(comando);
        assertNotNull(modulo, comando + " no es un comando privado");
        return rol.puede(modulo, comando);
    }

    @Test
    void yaNoSePuedenCrearRoles() {
        for (String comando : List.of("INSROL", "MODROL", "ELIROL")) {
            assertFalse(privados().containsKey(comando), comando);
        }
        assertEquals(3, Rol.values().length);
    }

    @Test
    void elAdministradorPuedeTodo() {
        privados().forEach((comando, modulo) -> assertTrue(ADMINISTRADOR.puede(modulo, comando), comando));
    }

    @Test
    void soloElAdministradorGestionaUsuariosPortalBitacoraYReportes() {
        for (String comando : List.of("LISUSU", "INSUSU", "LISROL", "MODINT", "MODCFG", "INSRED", "INSDES", "LISBIT",
                "REPGEN", "REPUSO")) {
            assertEquals(List.of(ADMINISTRADOR), Rol.quePueden(privados().get(comando), comando), comando);
        }
    }

    @Test
    void elEditorInstitucionalGestionaLoAcademicoYLaVinculacion() {
        for (String comando : List.of("INSINF", "INSMAT", "INSDOC", "INSPRO", "INSREP", "INSREV", "INSART", "INSCNV",
                "MODINN", "ATESOL", "EXPGRA", "LISMSJ", "RESMSJ", "INSACC", "ELIACC")) {
            assertTrue(puede(EDITOR_INSTITUCIONAL, comando), comando);
            assertFalse(puede(EDITOR_COMUNICACION, comando), comando);
        }
    }

    @Test
    void elEditorDeComunicacionGestionaNoticiasEventosYBanners() {
        for (String comando : List.of("INSNOT", "INSCOM", "INSEVT", "INSGAL", "INSBAN", "MODBAN", "ELIBAN", "LISBAN")) {
            assertTrue(puede(EDITOR_COMUNICACION, comando), comando);
            assertFalse(puede(EDITOR_INSTITUCIONAL, comando), comando);
        }
    }

    /** Los comandos sueltos de cada rol existen y son privados (un error de tipeo dejaría el permiso sin efecto). */
    @Test
    void losComandosSueltosDeCadaRolExisten() {
        for (Rol rol : Rol.values()) {
            for (String comando : rol.comandos()) {
                assertTrue(privados().containsKey(comando), rol + ": " + comando);
            }
        }
    }

    @Test
    void laAyudaNombraLosRoles() {
        assertEquals("Administrador y Editor de comunicación", Rol.nombres(Rol.quePueden("CU6", "INSNOT")));
        assertEquals("Administrador", Rol.nombres(Rol.quePueden("CU1", "LISUSU")));
    }
}
