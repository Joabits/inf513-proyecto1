-- =====================================================================
-- CU4 Investigaciones, revista INNOVAR y repositorio documental (RF-06, RF-07, RF-08)
-- Se puede ejecutar varias veces: crea lo que falta y no duplica datos.
-- Los archivos (PDF) se publican en el servidor web de la materia y aquí se guarda su enlace.
-- =====================================================================

-- Líneas de investigación de la Carrera (RF-07)
CREATE TABLE IF NOT EXISTS investigacion_linea (
    id          SERIAL        PRIMARY KEY,
    nombre      VARCHAR(160)  NOT NULL UNIQUE,
    descripcion VARCHAR(1000),
    creado      TIMESTAMP     NOT NULL DEFAULT now()
);

-- Proyectos de investigación, de investigación en aula (PIA) y de semillero (RF-07)
CREATE TABLE IF NOT EXISTS investigacion_proyecto (
    id          SERIAL        PRIMARY KEY,
    titulo      VARCHAR(200)  NOT NULL,
    tipo        VARCHAR(15)   NOT NULL CHECK (tipo IN ('INVESTIGACION', 'AULA', 'SEMILLERO')),
    linea_id    INTEGER       REFERENCES investigacion_linea (id) ON DELETE RESTRICT,
    responsable VARCHAR(160)  NOT NULL,
    integrantes VARCHAR(500),
    gestion     SMALLINT      NOT NULL CHECK (gestion BETWEEN 1970 AND 2100),
    estado      VARCHAR(10)   NOT NULL DEFAULT 'EN_CURSO' CHECK (estado IN ('EN_CURSO', 'CONCLUIDO')),
    resumen     VARCHAR(1000) NOT NULL,
    publicado   BOOLEAN       NOT NULL DEFAULT TRUE,
    creado      TIMESTAMP     NOT NULL DEFAULT now(),
    actualizado TIMESTAMP
);

-- Repositorio de documentos: reglamentos, resoluciones, formularios... (RF-06)
CREATE TABLE IF NOT EXISTS documento (
    id          SERIAL        PRIMARY KEY,
    titulo      VARCHAR(160)  NOT NULL,
    descripcion VARCHAR(300),
    categoria   VARCHAR(20)   NOT NULL CHECK (categoria IN ('REGLAMENTO', 'RESOLUCION', 'FORMULARIO', 'PLAN_DE_ESTUDIOS', 'INFORME', 'OTRO')),
    enlace      VARCHAR(500)  NOT NULL CHECK (enlace ~ '^https?://'),
    publicado   BOOLEAN       NOT NULL DEFAULT TRUE,
    creado      TIMESTAMP     NOT NULL DEFAULT now(),
    actualizado TIMESTAMP
);

-- Revista INNOVAR: ediciones (una nueva queda en borrador hasta publicarla) y sus artículos (RF-08)
CREATE TABLE IF NOT EXISTS revista_edicion (
    id          SERIAL        PRIMARY KEY,
    volumen     SMALLINT      CHECK (volumen BETWEEN 1 AND 99),
    numero      SMALLINT      NOT NULL CHECK (numero BETWEEN 1 AND 999),
    anio        SMALLINT      NOT NULL CHECK (anio BETWEEN 1990 AND 2100),
    titulo      VARCHAR(160),
    descripcion VARCHAR(1000),
    enlace_pdf  VARCHAR(500)  CHECK (enlace_pdf ~ '^https?://'),
    publicada   BOOLEAN       NOT NULL DEFAULT FALSE,
    creado      TIMESTAMP     NOT NULL DEFAULT now(),
    UNIQUE (anio, numero)
);

CREATE TABLE IF NOT EXISTS revista_articulo (
    id          SERIAL        PRIMARY KEY,
    edicion_id  INTEGER       NOT NULL REFERENCES revista_edicion (id) ON DELETE CASCADE,
    titulo      VARCHAR(200)  NOT NULL,
    autores     VARCHAR(300)  NOT NULL,
    resumen     VARCHAR(1000),
    enlace_pdf  VARCHAR(500)  CHECK (enlace_pdf ~ '^https?://'),
    orden       SMALLINT      NOT NULL DEFAULT 0,
    creado      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS revista_articulo_edicion_idx ON revista_articulo (edicion_id, orden);

-- ---------------------------------------------------------------------
-- Datos iniciales: líneas de investigación de la Carrera (se cambian con MODLIN)
-- ---------------------------------------------------------------------
INSERT INTO investigacion_linea (nombre) VALUES
('Gestión Estratégica, Innovación y Prospectiva Organizacional'),
('Emprendimiento, Desarrollo Empresarial y Ecosistemas Productivos'),
('Gestión del Talento Humano, Cultura Organizacional y Liderazgo')
ON CONFLICT (nombre) DO NOTHING;
