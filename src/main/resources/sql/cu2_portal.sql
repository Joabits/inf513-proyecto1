-- =====================================================================
-- CU2 Portal Web y Bitácora de Auditoría (RF-01, RF-02, RF-16, RF-19)
-- Se puede ejecutar varias veces: crea lo que falta y no duplica datos.
-- =====================================================================

-- Datos configurables del portal: bienvenida (RF-01) y contacto (RF-16)
CREATE TABLE IF NOT EXISTS configuracion (
    clave        VARCHAR(40)  PRIMARY KEY,
    valor        TEXT         NOT NULL DEFAULT '',
    grupo        VARCHAR(20)  NOT NULL,
    descripcion  VARCHAR(150) NOT NULL,
    actualizado  TIMESTAMP    NOT NULL DEFAULT now()
);

-- Carrusel de imágenes de la página de inicio (RF-01)
CREATE TABLE IF NOT EXISTS banner (
    id          SERIAL       PRIMARY KEY,
    titulo      VARCHAR(150) NOT NULL,
    subtitulo   VARCHAR(250),
    imagen_url  VARCHAR(500) NOT NULL,
    enlace      VARCHAR(500),
    orden       INTEGER      NOT NULL DEFAULT 1 CHECK (orden > 0),
    creado      TIMESTAMP    NOT NULL DEFAULT now()
);

-- Contenidos destacados de la página de inicio (RF-01)
CREATE TABLE IF NOT EXISTS destacado (
    id          SERIAL       PRIMARY KEY,
    titulo      VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    enlace      VARCHAR(500),
    orden       INTEGER      NOT NULL DEFAULT 1 CHECK (orden > 0),
    creado      TIMESTAMP    NOT NULL DEFAULT now()
);

-- Accesos rápidos a las plataformas de la Universidad (RF-01)
CREATE TABLE IF NOT EXISTS acceso_rapido (
    id          SERIAL       PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    url         VARCHAR(500) NOT NULL,
    descripcion VARCHAR(250),
    orden       INTEGER      NOT NULL DEFAULT 1 CHECK (orden > 0)
);

-- Información institucional (RF-02)
CREATE TABLE IF NOT EXISTS seccion_institucional (
    clave       VARCHAR(40)  PRIMARY KEY,
    titulo      VARCHAR(150) NOT NULL,
    contenido   TEXT         NOT NULL,
    orden       INTEGER      NOT NULL DEFAULT 1,
    actualizado TIMESTAMP    NOT NULL DEFAULT now()
);

-- Redes sociales oficiales (RF-16)
CREATE TABLE IF NOT EXISTS red_social (
    id          SERIAL       PRIMARY KEY,
    nombre      VARCHAR(50)  NOT NULL UNIQUE,
    url         VARCHAR(500) NOT NULL,
    orden       INTEGER      NOT NULL DEFAULT 1 CHECK (orden > 0)
);

-- Bitácora de auditoría (RF-19): la usan todos los casos de uso
CREATE TABLE IF NOT EXISTS bitacora (
    id          BIGSERIAL    PRIMARY KEY,
    fecha       TIMESTAMP    NOT NULL DEFAULT now(),
    usuario     VARCHAR(120) NOT NULL,
    accion      VARCHAR(20)  NOT NULL,
    modulo      VARCHAR(5)   NOT NULL,
    tabla       VARCHAR(60)  NOT NULL,
    elemento    VARCHAR(60),
    detalle     TEXT
);
CREATE INDEX IF NOT EXISTS bitacora_fecha_idx ON bitacora (fecha);

-- ---------------------------------------------------------------------
-- Datos iniciales (los textos de ejemplo se cambian con MODCFG y MODINT)
-- ---------------------------------------------------------------------
INSERT INTO configuracion (clave, valor, grupo, descripcion) VALUES
('titulo_portal', 'Carrera de Administración de Empresas - UAGRM', 'inicio', 'Nombre del portal'),
('mensaje_bienvenida', 'Bienvenido al portal de la Carrera de Administración de Empresas de la Universidad Autónoma Gabriel René Moreno. Aquí encontrarás la información académica, las noticias, los eventos y los servicios de la Carrera.', 'inicio', 'Mensaje de bienvenida de la página de inicio'),
('direccion', 'Ciudad Universitaria UAGRM, Facultad de Ciencias Económicas, Santa Cruz de la Sierra - Bolivia', 'contacto', 'Dirección'),
('mapa_url', 'https://www.google.com/maps/search/?api=1&query=Universidad+Autonoma+Gabriel+Rene+Moreno+Santa+Cruz', 'contacto', 'Ubicación en el mapa'),
('telefonos', '(591-3) 000-0000 (dato de ejemplo)', 'contacto', 'Teléfonos'),
('correos', 'contacto@ejemplo.edu.bo (dato de ejemplo)', 'contacto', 'Correos'),
('horario_atencion', 'Lunes a viernes de 08:00 a 12:00 y de 14:00 a 18:00', 'contacto', 'Horario de atención')
ON CONFLICT (clave) DO NOTHING;

INSERT INTO seccion_institucional (clave, titulo, contenido, orden) VALUES
('historia', 'Historia', 'La Carrera de Administración de Empresas forma parte de la Universidad Autónoma Gabriel René Moreno y prepara profesionales para la gestión de organizaciones públicas y privadas de la región. (Texto de ejemplo.)', 1),
('mision', 'Misión', 'Formar profesionales en Administración de Empresas con sólidos conocimientos científicos, técnicos y éticos, capaces de dirigir organizaciones de manera eficiente y de contribuir al desarrollo económico y social de Santa Cruz y de Bolivia. (Texto de ejemplo.)', 2),
('vision', 'Visión', 'Ser una carrera acreditada y referente a nivel nacional en la formación de administradores emprendedores e innovadores, vinculada con el sector productivo y la investigación. (Texto de ejemplo.)', 3),
('objetivos', 'Objetivos', 'Formar profesionales con capacidad de planificar, organizar, dirigir y controlar organizaciones; promover la investigación y el emprendimiento; y fortalecer la vinculación con empresas e instituciones. (Texto de ejemplo.)', 4),
('perfil_profesional', 'Perfil profesional', 'El Licenciado en Administración de Empresas diagnostica, planifica y dirige organizaciones, toma decisiones basadas en información financiera y de mercado, lidera equipos de trabajo y emprende nuevos negocios con responsabilidad social. (Texto de ejemplo.)', 5),
('campo_ocupacional', 'Campo ocupacional', 'Empresas privadas, instituciones públicas, entidades financieras, organizaciones no gubernamentales, consultoría independiente y emprendimientos propios. (Texto de ejemplo.)', 6),
('estructura_organizativa', 'Estructura organizativa', 'Dirección de Carrera, Consejo de Carrera, Coordinación Académica, Unidad de Investigación, Unidad de Interacción Social y Vinculación, y Kardex. (Texto de ejemplo.)', 7)
ON CONFLICT (clave) DO NOTHING;

-- Los datos de ejemplo siguientes solo se cargan si la tabla está vacía
INSERT INTO banner (titulo, subtitulo, imagen_url, enlace, orden)
SELECT v.titulo, v.subtitulo, v.imagen_url, v.enlace, v.orden FROM (VALUES
('Inscripciones abiertas', 'Conoce los requisitos de admisión a la Carrera', 'https://www.tecnoweb.org.bo/inf513/grupo17sa/img/banner-inscripciones.jpg', 'https://www.uagrm.edu.bo', 1),
('Revista INNOVAR', 'Publica tu artículo en la revista académica de la Carrera', 'https://www.tecnoweb.org.bo/inf513/grupo17sa/img/banner-innovar.jpg', NULL, 2),
('Empresa Junior INNOVA', 'Consultoría estudiantil para micro y pequeñas empresas', 'https://www.tecnoweb.org.bo/inf513/grupo17sa/img/banner-innova.jpg', NULL, 3)
) AS v(titulo, subtitulo, imagen_url, enlace, orden)
WHERE NOT EXISTS (SELECT 1 FROM banner);

INSERT INTO destacado (titulo, descripcion, enlace, orden)
SELECT v.titulo, v.descripcion, v.enlace, v.orden FROM (VALUES
('Malla curricular', 'Consulta las asignaturas de cada semestre y sus prerrequisitos.', NULL, 1),
('Revista INNOVAR', 'Lee las ediciones de la revista académica de la Carrera.', NULL, 2),
('Seguimiento a graduados', 'Si eres graduado, registra tus datos en el programa Alumni.', NULL, 3)
) AS v(titulo, descripcion, enlace, orden)
WHERE NOT EXISTS (SELECT 1 FROM destacado);

INSERT INTO acceso_rapido (nombre, url, descripcion, orden)
SELECT v.nombre, v.url, v.descripcion, v.orden FROM (VALUES
('Portal UAGRM', 'https://www.uagrm.edu.bo', 'Sitio oficial de la Universidad', 1),
('Aula virtual', 'https://presencial.uagrm.edu.bo', 'Plataforma Moodle de las materias', 2)
) AS v(nombre, url, descripcion, orden)
WHERE NOT EXISTS (SELECT 1 FROM acceso_rapido);

INSERT INTO red_social (nombre, url, orden)
SELECT v.nombre, v.url, v.orden FROM (VALUES
('Facebook', 'https://www.facebook.com/', 1),
('Instagram', 'https://www.instagram.com/', 2),
('YouTube', 'https://www.youtube.com/', 3)
) AS v(nombre, url, orden)
WHERE NOT EXISTS (SELECT 1 FROM red_social);
