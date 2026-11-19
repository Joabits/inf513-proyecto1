-- =====================================================================
-- CU5 Vinculación, Empresa Junior INNOVA y Alumni (RF-09, RF-10, RF-11)
-- Se puede ejecutar varias veces: crea lo que falta y no duplica datos.
-- Requiere la tabla configuracion del CU2 (se instala antes).
-- =====================================================================

-- Convenios, prácticas, proyectos con empresas, consultorías e interacción social (RF-09)
CREATE TABLE IF NOT EXISTS convenio (
    id            SERIAL       PRIMARY KEY,
    tipo          VARCHAR(20)  NOT NULL CHECK (tipo IN ('CONVENIO', 'PRACTICA', 'PROYECTO', 'CONSULTORIA', 'INTERACCION')),
    institucion   VARCHAR(200) NOT NULL,
    objeto        VARCHAR(500) NOT NULL,
    fecha_inicio  DATE         NOT NULL,
    fecha_fin     DATE,
    estado        VARCHAR(15)  NOT NULL DEFAULT 'VIGENTE' CHECK (estado IN ('VIGENTE', 'EN_TRAMITE', 'CONCLUIDO')),
    responsable   VARCHAR(150),
    logo_url      VARCHAR(500),
    creado        TIMESTAMP    NOT NULL DEFAULT now(),
    usuario_id    INTEGER      REFERENCES usuario (id) ON DELETE SET NULL,
    CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

-- Empresa Junior INNOVA: servicios, equipo y proyectos realizados (RF-10)
CREATE TABLE IF NOT EXISTS innova_servicio (
    id          SERIAL       PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    usuario_id  INTEGER      REFERENCES usuario (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS innova_miembro (
    id          SERIAL       PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    cargo       VARCHAR(100) NOT NULL,
    semestre    VARCHAR(30),
    correo      VARCHAR(120),
    usuario_id  INTEGER      REFERENCES usuario (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS innova_proyecto (
    id          SERIAL       PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL,
    cliente     VARCHAR(200),
    descripcion VARCHAR(500) NOT NULL,
    anio        INTEGER      NOT NULL CHECK (anio BETWEEN 2000 AND 2100),
    resultado   VARCHAR(500),
    usuario_id  INTEGER      REFERENCES usuario (id) ON DELETE SET NULL
);

-- Solicitudes de servicio de empresas y postulaciones de estudiantes a INNOVA (RF-10)
CREATE TABLE IF NOT EXISTS innova_solicitud (
    id            SERIAL        PRIMARY KEY,
    tipo          VARCHAR(12)   NOT NULL CHECK (tipo IN ('SERVICIO', 'POSTULACION')),
    nombre        VARCHAR(150)  NOT NULL,
    correo        VARCHAR(120)  NOT NULL,
    telefono      VARCHAR(30),
    organizacion  VARCHAR(200),
    mensaje       VARCHAR(1000) NOT NULL,
    estado        VARCHAR(10)   NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'ATENDIDA', 'RECHAZADA')),
    observacion   VARCHAR(1000),
    fecha         TIMESTAMP     NOT NULL DEFAULT now(),
    atendido_por  VARCHAR(120),
    atendido_en   TIMESTAMP,
    usuario_id    INTEGER       REFERENCES usuario (id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS innova_solicitud_correo_idx ON innova_solicitud (correo);

-- Registro y seguimiento de graduados (RF-11). El correo es el del remitente: un registro por correo.
CREATE TABLE IF NOT EXISTS graduado (
    id              SERIAL       PRIMARY KEY,
    correo          VARCHAR(120) NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    apellidos       VARCHAR(100) NOT NULL,
    telefono        VARCHAR(30),
    anio_titulacion INTEGER      NOT NULL CHECK (anio_titulacion BETWEEN 1960 AND 2100),
    empleo_actual   VARCHAR(200),
    cargo           VARCHAR(120),
    sector          VARCHAR(15)  NOT NULL CHECK (sector IN ('PUBLICO', 'PRIVADO', 'INDEPENDIENTE', 'ONG', 'SIN_EMPLEO')),
    ciudad          VARCHAR(80),
    registrado      TIMESTAMP    NOT NULL DEFAULT now(),
    actualizado     TIMESTAMP    NOT NULL DEFAULT now(),
    usuario_id      INTEGER      REFERENCES usuario (id) ON DELETE SET NULL
);

-- Responsable de cada registro (FK a usuario): el usuario que lo creó o modificó por última vez.
-- La llena BitacoraNegocio al registrar la acción. Si la base se instaló antes, aquí se agrega la columna.
ALTER TABLE convenio ADD COLUMN IF NOT EXISTS usuario_id INTEGER REFERENCES usuario (id) ON DELETE SET NULL;
ALTER TABLE innova_servicio ADD COLUMN IF NOT EXISTS usuario_id INTEGER REFERENCES usuario (id) ON DELETE SET NULL;
ALTER TABLE innova_miembro ADD COLUMN IF NOT EXISTS usuario_id INTEGER REFERENCES usuario (id) ON DELETE SET NULL;
ALTER TABLE innova_proyecto ADD COLUMN IF NOT EXISTS usuario_id INTEGER REFERENCES usuario (id) ON DELETE SET NULL;
ALTER TABLE innova_solicitud ADD COLUMN IF NOT EXISTS usuario_id INTEGER REFERENCES usuario (id) ON DELETE SET NULL;
ALTER TABLE graduado ADD COLUMN IF NOT EXISTS usuario_id INTEGER REFERENCES usuario (id) ON DELETE SET NULL;

-- ---------------------------------------------------------------------
-- Datos iniciales y de ejemplo (nombres «Demo» y correos @example.com son ficticios)
-- ---------------------------------------------------------------------
INSERT INTO configuracion (clave, valor, grupo, descripcion) VALUES
('innova_descripcion', 'La Empresa Junior INNOVA es una organización de estudiantes de la Carrera que ofrece servicios de consultoría a micro y pequeñas empresas, bajo la supervisión de docentes, para aplicar en la práctica lo aprendido en las aulas.', 'innova', 'Descripción de la Empresa Junior INNOVA')
ON CONFLICT (clave) DO NOTHING;

INSERT INTO convenio (tipo, institucion, objeto, fecha_inicio, fecha_fin, estado, responsable)
SELECT v.tipo, v.institucion, v.objeto, v.fecha_inicio::date, v.fecha_fin::date, v.estado, v.responsable FROM (VALUES
('CONVENIO', 'Cámara de Comercio de Ejemplo', 'Cooperación interinstitucional para pasantías y capacitación.', '2025-01-15', '2027-01-14', 'VIGENTE', 'Dirección de Carrera'),
('PRACTICA', 'Distribuidora Oriente Demo S.R.L.', 'Prácticas empresariales para estudiantes de 9no semestre.', '2026-02-01', '2026-12-31', 'VIGENTE', 'Coordinación Académica'),
('CONSULTORIA', 'Asociación de Artesanos Demo', 'Consultoría en costos y comercialización.', '2025-05-10', '2025-11-30', 'CONCLUIDO', 'Empresa Junior INNOVA'),
('INTERACCION', 'Municipio de Ejemplo', 'Talleres de educación financiera para emprendedores.', '2026-09-01', NULL, 'EN_TRAMITE', 'Unidad de Interacción Social')
) AS v(tipo, institucion, objeto, fecha_inicio, fecha_fin, estado, responsable)
WHERE NOT EXISTS (SELECT 1 FROM convenio);

INSERT INTO innova_servicio (nombre, descripcion)
SELECT v.nombre, v.descripcion FROM (VALUES
('Plan de negocios', 'Elaboración de planes de negocio para nuevos emprendimientos.'),
('Estudio de mercado', 'Encuestas y análisis del mercado objetivo.'),
('Costos y precios', 'Cálculo de costos y fijación de precios para microempresas.')
) AS v(nombre, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM innova_servicio);

INSERT INTO innova_miembro (nombre, cargo, semestre, correo)
SELECT v.nombre, v.cargo, v.semestre, v.correo FROM (VALUES
('Luciana Arteaga', 'Gerente general', '8vo semestre', 'larteaga@example.com'),
('Mateo Céspedes', 'Responsable de proyectos', '7mo semestre', 'mcespedes@example.com'),
('Valeria Roca', 'Responsable de marketing', '6to semestre', 'vroca@example.com')
) AS v(nombre, cargo, semestre, correo)
WHERE NOT EXISTS (SELECT 1 FROM innova_miembro);

INSERT INTO innova_proyecto (nombre, cliente, descripcion, anio, resultado)
SELECT v.nombre, v.cliente, v.descripcion, v.anio, v.resultado FROM (VALUES
('Plan de negocios para una panadería', 'Panadería Demo', 'Plan de negocios para ampliar la producción.', 2025, 'Plan entregado y aprobado por el cliente.'),
('Estudio de mercado de café local', 'Café Demo', 'Estudio de preferencias de consumidores universitarios.', 2026, 'Informe con recomendaciones de precio y canal.')
) AS v(nombre, cliente, descripcion, anio, resultado)
WHERE NOT EXISTS (SELECT 1 FROM innova_proyecto);

INSERT INTO innova_solicitud (tipo, nombre, correo, telefono, organizacion, mensaje)
SELECT v.tipo, v.nombre, v.correo, v.telefono, v.organizacion, v.mensaje FROM (VALUES
('SERVICIO', 'Rosa Pinto', 'rosa.pinto@example.com', '70000001', 'Tienda Demo', 'Necesitamos ayuda para calcular nuestros costos.'),
('POSTULACION', 'Diego Salazar', 'diego.salazar@example.com', '70000002', '5to semestre', 'Quiero formar parte del equipo de INNOVA.')
) AS v(tipo, nombre, correo, telefono, organizacion, mensaje)
WHERE NOT EXISTS (SELECT 1 FROM innova_solicitud);

INSERT INTO graduado (correo, nombre, apellidos, telefono, anio_titulacion, empleo_actual, cargo, sector, ciudad)
SELECT v.correo, v.nombre, v.apellidos, v.telefono, v.anio, v.empleo, v.cargo, v.sector, v.ciudad FROM (VALUES
('andrea.lopez@example.com', 'Andrea', 'López Ortiz', '71000001', 2019, 'Banco Demo S.A.', 'Analista de créditos', 'PRIVADO', 'Santa Cruz'),
('marcelo.rojas@example.com', 'Marcelo', 'Rojas Flores', '71000002', 2020, 'Gobierno Municipal Demo', 'Técnico administrativo', 'PUBLICO', 'Santa Cruz'),
('gabriela.torrico@example.com', 'Gabriela', 'Torrico Vaca', '71000003', 2021, 'Consultora propia', 'Gerente', 'INDEPENDIENTE', 'Cochabamba'),
('pablo.anez@example.com', 'Pablo', 'Añez Suárez', '71000004', 2022, 'Industria Demo S.A.', 'Jefe de compras', 'PRIVADO', 'Santa Cruz'),
('lucia.herrera@example.com', 'Lucía', 'Herrera Paz', '71000005', 2023, 'Fundación Demo', 'Coordinadora de proyectos', 'ONG', 'La Paz'),
('ivan.chavez@example.com', 'Iván', 'Chávez Rivero', '71000006', 2024, NULL, NULL, 'SIN_EMPLEO', 'Santa Cruz')
) AS v(correo, nombre, apellidos, telefono, anio, empleo, cargo, sector, ciudad)
WHERE NOT EXISTS (SELECT 1 FROM graduado);
