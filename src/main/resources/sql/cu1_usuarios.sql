-- =====================================================================
-- CU1 Gestión de Usuarios, roles y permisos (RF-18)
-- Se puede ejecutar varias veces: crea lo que falta y no duplica datos.
-- Un correo tiene permiso sobre un caso de uso si es un usuario activo y su rol
-- tiene ese caso de uso (o '*', todos). Los correos de admin.correos (config.properties)
-- son administradores de arranque: sirven para crear el primer usuario.
-- =====================================================================

-- Roles del sistema (el nombre se guarda en mayúsculas, por ejemplo EDITOR_INSTITUCIONAL)
CREATE TABLE IF NOT EXISTS rol (
    id          SERIAL       PRIMARY KEY,
    nombre      VARCHAR(40)  NOT NULL UNIQUE CHECK (nombre = upper(nombre)),
    descripcion VARCHAR(200),
    creado      TIMESTAMP    NOT NULL DEFAULT now()
);

-- Casos de uso que puede administrar cada rol ('*' = todos)
CREATE TABLE IF NOT EXISTS rol_modulo (
    rol_id      INTEGER      NOT NULL REFERENCES rol (id) ON DELETE CASCADE,
    modulo      VARCHAR(5)   NOT NULL CHECK (modulo IN ('*', 'CU1', 'CU2', 'CU3', 'CU4', 'CU5', 'CU6', 'CU7', 'CU8')),
    PRIMARY KEY (rol_id, modulo)
);

-- Usuarios: se identifican por el correo desde el que envían los comandos
CREATE TABLE IF NOT EXISTS usuario (
    id          SERIAL       PRIMARY KEY,
    correo      VARCHAR(120) NOT NULL UNIQUE CHECK (correo = lower(correo)),
    nombre      VARCHAR(150) NOT NULL,
    rol_id      INTEGER      NOT NULL REFERENCES rol (id) ON DELETE RESTRICT,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado      TIMESTAMP    NOT NULL DEFAULT now(),
    actualizado TIMESTAMP
);

CREATE INDEX IF NOT EXISTS usuario_rol_idx ON usuario (rol_id);

-- ---------------------------------------------------------------------
-- Roles iniciales (se cambian con MODROL; los usuarios se crean con INSUSU)
-- ---------------------------------------------------------------------
INSERT INTO rol (nombre, descripcion) VALUES
('ADMINISTRADOR', 'Director de Carrera: administra todos los casos de uso'),
('EDITOR_INSTITUCIONAL', 'Portal, investigación, revista, documentos y vinculación'),
('EDITOR_COMUNICACION', 'Atiende los mensajes de contacto'),
('CONSULTOR', 'Consulta los reportes y estadísticas')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol_modulo (rol_id, modulo)
SELECT r.id, m.modulo
FROM rol r
JOIN (VALUES ('ADMINISTRADOR', '*'),
             ('EDITOR_INSTITUCIONAL', 'CU2'), ('EDITOR_INSTITUCIONAL', 'CU4'), ('EDITOR_INSTITUCIONAL', 'CU5'),
             ('EDITOR_COMUNICACION', 'CU7'),
             ('CONSULTOR', 'CU8')) AS m (rol, modulo) ON m.rol = r.nombre
ON CONFLICT DO NOTHING;
