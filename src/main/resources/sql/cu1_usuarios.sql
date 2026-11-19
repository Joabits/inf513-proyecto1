-- =====================================================================
-- CU1 Gestión de Usuarios (RF-18)
-- Se puede ejecutar varias veces: crea lo que falta y no duplica datos.
-- Los roles son fijos (no se crean ni se modifican por correo) y lo que gestiona cada uno está en la clase Rol:
--   ADMINISTRADOR         Director de Carrera: gestiona todo
--   EDITOR_INSTITUCIONAL  oferta académica, malla, docentes, investigación, documentos, revista INNOVAR,
--                         vinculación, INNOVA, Alumni, mensajes de contacto y accesos rápidos
--   EDITOR_COMUNICACION   noticias, comunicados, eventos, galería y banners
-- Quien no tiene usuario activo es Visitante: solo usa los comandos públicos.
-- Los correos de admin.correos (config.properties) son administradores de arranque: sirven para crear el primer usuario.
-- =====================================================================

-- Usuarios: se identifican por el correo desde el que envían los comandos
CREATE TABLE IF NOT EXISTS usuario (
    id          SERIAL       PRIMARY KEY,
    correo      VARCHAR(120) NOT NULL UNIQUE CHECK (correo = lower(correo)),
    nombre      VARCHAR(150) NOT NULL,
    rol         VARCHAR(30)  NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'EDITOR_INSTITUCIONAL', 'EDITOR_COMUNICACION')),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado      TIMESTAMP    NOT NULL DEFAULT now(),
    actualizado TIMESTAMP
);

-- ---------------------------------------------------------------------
-- Bases instaladas con la versión anterior (roles dinámicos en las tablas rol y rol_modulo):
-- cada usuario conserva su rol si es uno de los tres fijos. Los de otro rol (por ejemplo CONSULTOR)
-- quedan como EDITOR_INSTITUCIONAL inactivos hasta que el administrador les asigne un rol con MODUSU.
-- ---------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'usuario' AND column_name = 'rol_id') THEN
        ALTER TABLE usuario ADD COLUMN rol VARCHAR(30);
        UPDATE usuario u SET rol = r.nombre FROM rol r WHERE r.id = u.rol_id;
        UPDATE usuario SET rol = 'EDITOR_INSTITUCIONAL', activo = FALSE, actualizado = now()
        WHERE rol IS NULL OR rol NOT IN ('ADMINISTRADOR', 'EDITOR_INSTITUCIONAL', 'EDITOR_COMUNICACION');
        ALTER TABLE usuario ALTER COLUMN rol SET NOT NULL;
        ALTER TABLE usuario ADD CHECK (rol IN ('ADMINISTRADOR', 'EDITOR_INSTITUCIONAL', 'EDITOR_COMUNICACION'));
        ALTER TABLE usuario DROP COLUMN rol_id;
    END IF;
END
$$;

DROP TABLE IF EXISTS rol_modulo;

DROP TABLE IF EXISTS rol;

CREATE INDEX IF NOT EXISTS usuario_rol_idx ON usuario (rol);
