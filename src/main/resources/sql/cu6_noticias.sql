-- =====================================================================
-- CU6 Noticias, Comunicados, Eventos y Galería (RF-07, RF-08, RF-17, RF-18)
-- Se puede ejecutar varias veces: crea lo que falta y no duplica datos.
-- =====================================================================

-- Noticias institucionales (RF-07)
CREATE TABLE IF NOT EXISTS noticia (
    id                SERIAL       PRIMARY KEY,
    titulo            VARCHAR(200) NOT NULL,
    resumen           TEXT         NOT NULL,
    contenido         TEXT         NOT NULL,
    imagen_url        VARCHAR(500),
    autor             VARCHAR(150) NOT NULL,
    fecha_publicacion TIMESTAMP    NOT NULL DEFAULT now(),
    publicado         BOOLEAN      NOT NULL DEFAULT true,
    orden             INTEGER      NOT NULL DEFAULT 1 CHECK (orden > 0),
    creado            TIMESTAMP    NOT NULL DEFAULT now()
);

-- Comunicados y avisos urgentes (RF-08)
CREATE TABLE IF NOT EXISTS comunicado (
    id                SERIAL       PRIMARY KEY,
    titulo            VARCHAR(200) NOT NULL,
    contenido         TEXT         NOT NULL,
    prioridad         VARCHAR(20)  NOT NULL CHECK (prioridad IN ('BAJA','MEDIA','ALTA','URGENTE')),
    fecha_publicacion TIMESTAMP    NOT NULL DEFAULT now(),
    fecha_expiracion  TIMESTAMP,
    creado            TIMESTAMP    NOT NULL DEFAULT now()
);

-- Agenda de eventos y actividades (RF-17)
CREATE TABLE IF NOT EXISTS evento (
    id           SERIAL       PRIMARY KEY,
    titulo       VARCHAR(200) NOT NULL,
    descripcion  TEXT         NOT NULL,
    fecha_inicio TIMESTAMP    NOT NULL,
    fecha_fin    TIMESTAMP,
    lugar        VARCHAR(200) NOT NULL,
    imagen_url   VARCHAR(500),
    creado       TIMESTAMP    NOT NULL DEFAULT now()
);

-- Galería de imágenes (RF-18)
CREATE TABLE IF NOT EXISTS galeria (
    id          SERIAL       PRIMARY KEY,
    titulo      VARCHAR(200) NOT NULL,
    descripcion TEXT,
    imagen_url  VARCHAR(500) NOT NULL,
    evento_id   INTEGER      REFERENCES evento(id) ON DELETE SET NULL,
    orden       INTEGER      NOT NULL DEFAULT 1 CHECK (orden > 0),
    creado      TIMESTAMP    NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- Datos iniciales de ejemplo
-- ---------------------------------------------------------------------
INSERT INTO noticia (titulo, resumen, contenido, imagen_url, autor, fecha_publicacion, publicado, orden)
SELECT v.titulo, v.resumen, v.contenido, v.imagen_url, v.autor, v.fecha_publicacion, v.publicado, v.orden FROM (VALUES
('Acreditación Internacional de la Carrera', 'La Carrera de Administración de Empresas inicia su proceso de autoevaluación.', 'La Carrera de Administración de Empresas de la UAGRM ha dado inicio formal al proceso de autoevaluación con fines de acreditación internacional ante organismos regionales.', 'https://img.freepik.com/foto-gratis/estudiantes-universidad-biblioteca_23-2148166348.jpg', 'Dirección de Carrera', now() - INTERVAL '2 days', true, 1),
('Feria Empresarial y de Emprendimiento 2026', 'Estudiantes de últimos semestres presentarán más de 40 planes de negocio.', 'Se invita a toda la comunidad universitaria a la Feria Empresarial Anual donde se expondrán proyectos en tecnología, servicios sostenibles y modelos de franquicia.', 'https://img.freepik.com/foto-gratis/reunion-negocios-oficina_23-2148898715.jpg', 'Comisión Académica', now() - INTERVAL '1 day', true, 2)
) AS v(titulo, resumen, contenido, imagen_url, autor, fecha_publicacion, publicado, orden)
WHERE NOT EXISTS (SELECT 1 FROM noticia);

INSERT INTO comunicado (titulo, contenido, prioridad, fecha_publicacion, fecha_expiracion)
SELECT v.titulo, v.contenido, v.prioridad, v.fecha_publicacion, v.fecha_expiracion FROM (VALUES
('Inscripción a Mesas de Examen', 'Se informa a los estudiantes que el periodo de inscripción para mesas examinadoras del presente semestre estará habilitado hasta fin de mes.', 'ALTA', now(), now() + INTERVAL '10 days'),
('Mantenimiento Programado del Servidor Web', 'El portal web institucional estuvo fuera de servicio temporalmente por tareas de mantenimiento.', 'MEDIA', now() - INTERVAL '30 days', now() - INTERVAL '20 days')
) AS v(titulo, contenido, prioridad, fecha_publicacion, fecha_expiracion)
WHERE NOT EXISTS (SELECT 1 FROM comunicado);

INSERT INTO evento (titulo, descripcion, fecha_inicio, fecha_fin, lugar, imagen_url)
SELECT v.titulo, v.descripcion, v.fecha_inicio, v.fecha_fin, v.lugar, v.imagen_url FROM (VALUES
('Congreso Internacional de Gestión Estratégica', 'Encuentro académico con expositores internacionales sobre transformación digital e innovación empresarial.', now() + INTERVAL '15 days', now() + INTERVAL '17 days', 'Auditorio Central de la Facultad', 'https://img.freepik.com/foto-gratis/conferencia-negocios-publico_23-2149178654.jpg'),
('Taller de Liderazgo y Trabajo en Equipo', 'Capacitación teórico-práctica para estudiantes y futuros egresados enfocada en habilidades directivas.', now() + INTERVAL '7 days', now() + INTERVAL '7 days' + INTERVAL '4 hours', 'Sala de Videoconferencias Bloque B', 'https://img.freepik.com/foto-gratis/equipo-trabajando-juntos-oficina_23-2148908920.jpg')
) AS v(titulo, descripcion, fecha_inicio, fecha_fin, lugar, imagen_url)
WHERE NOT EXISTS (SELECT 1 FROM evento);

INSERT INTO galeria (titulo, descripcion, imagen_url, evento_id, orden)
SELECT v.titulo, v.descripcion, v.imagen_url, e.id, v.orden FROM (VALUES
('Apertura del Congreso Internacional', 'Acto inaugural con autoridades facultativas y disertantes invitados.', 'https://img.freepik.com/foto-gratis/conferencia-apertura_23-2149178660.jpg', 'Congreso Internacional de Gestión Estratégica', 1),
('Dinámica de Grupo en Taller', 'Estudiantes participando en la sesión de negociación del taller.', 'https://img.freepik.com/foto-gratis/taller-liderazgo_23-2148908930.jpg', 'Taller de Liderazgo y Trabajo en Equipo', 2),
('Frontis del Edificio de Administración', 'Instalaciones centrales de la Carrera de Administración de Empresas.', 'https://img.freepik.com/foto-gratis/edificio-universitario_23-2148166350.jpg', NULL, 3),
('Biblioteca Especializada de Ciencias Económicas', 'Espacio de estudio y consulta bibliográfica para estudiantes.', 'https://img.freepik.com/foto-gratis/biblioteca-estudio_23-2148166360.jpg', NULL, 4)
) AS v(titulo, descripcion, imagen_url, titulo_evento, orden)
LEFT JOIN evento e ON e.titulo = v.titulo_evento
WHERE NOT EXISTS (SELECT 1 FROM galeria);
