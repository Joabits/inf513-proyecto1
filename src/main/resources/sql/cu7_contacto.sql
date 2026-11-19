-- =====================================================================
-- CU7 Formulario de Contacto y Búsqueda Interna Global (RF-15, RF-17)
-- Se puede ejecutar varias veces: crea lo que falta y no duplica datos.
-- La búsqueda global (BUSCAR) no tiene tablas propias: consulta las de los demás casos de uso.
-- =====================================================================

-- Mensajes que los visitantes envían a la Carrera (RF-15). El correo es el del remitente del comando.
CREATE TABLE IF NOT EXISTS mensaje_contacto (
    id              SERIAL        PRIMARY KEY,
    correo          VARCHAR(120)  NOT NULL CHECK (correo = lower(correo)),
    nombre          VARCHAR(150)  NOT NULL,
    telefono        VARCHAR(30),
    asunto          VARCHAR(150)  NOT NULL,
    mensaje         VARCHAR(1000) NOT NULL,
    estado          VARCHAR(12)   NOT NULL DEFAULT 'NUEVO' CHECK (estado IN ('NUEVO', 'LEIDO', 'RESPONDIDO', 'ARCHIVADO')),
    respuesta       VARCHAR(1000),
    respondido_por  VARCHAR(120),
    respondido_en   TIMESTAMP,
    fecha           TIMESTAMP     NOT NULL DEFAULT now(),
    usuario_id      INTEGER       REFERENCES usuario (id) ON DELETE SET NULL,
    CHECK (estado <> 'RESPONDIDO' OR respuesta IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS mensaje_contacto_correo_idx ON mensaje_contacto (correo);
CREATE INDEX IF NOT EXISTS mensaje_contacto_estado_idx ON mensaje_contacto (estado, fecha);

-- Responsable de cada registro (FK a usuario): el usuario que lo creó o modificó por última vez.
-- La llena BitacoraNegocio al registrar la acción. Si la base se instaló antes, aquí se agrega la columna.
ALTER TABLE mensaje_contacto ADD COLUMN IF NOT EXISTS usuario_id INTEGER REFERENCES usuario (id) ON DELETE SET NULL;
