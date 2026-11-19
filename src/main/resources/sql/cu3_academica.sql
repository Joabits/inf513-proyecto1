-- =====================================================================
-- CU3 Información Académica, Malla Interactiva y Plantel Docente
-- Se puede ejecutar varias veces: crea lo que falta y no duplica datos.
-- =====================================================================

-- Información académica general y reglamentos (RF-03, RF-04)
CREATE TABLE IF NOT EXISTS info_academica (
    id          SERIAL       PRIMARY KEY,
    titulo      VARCHAR(150) NOT NULL,
    contenido   TEXT         NOT NULL,
    orden       INTEGER      NOT NULL DEFAULT 1 CHECK (orden > 0),
    actualizado TIMESTAMP    NOT NULL DEFAULT now()
);

-- Malla curricular de asignaturas de la Carrera (RF-05)
CREATE TABLE IF NOT EXISTS materia (
    id            SERIAL       PRIMARY KEY,
    sigla         VARCHAR(20)  NOT NULL UNIQUE,
    nombre        VARCHAR(150) NOT NULL,
    semestre      INTEGER      NOT NULL CHECK (semestre > 0),
    creditos      INTEGER      NOT NULL DEFAULT 0 CHECK (creditos >= 0),
    prerrequisito VARCHAR(150)
);

-- Plantel docente y asignación de materias (RF-06)
CREATE TABLE IF NOT EXISTS docente (
    id         SERIAL       PRIMARY KEY,
    nombre     VARCHAR(150) NOT NULL,
    titulo     VARCHAR(150),
    cargo      VARCHAR(100),
    correo     VARCHAR(120),
    materia_id INTEGER      REFERENCES materia(id) ON DELETE SET NULL
);

-- ---------------------------------------------------------------------
-- Datos iniciales de ejemplo
-- ---------------------------------------------------------------------
INSERT INTO info_academica (titulo, contenido, orden)
SELECT v.titulo, v.contenido, v.orden FROM (VALUES
('Plan de Estudios', 'El plan de estudios de la Carrera de Administración de Empresas está estructurado en semestres con asignaturas en las áreas de Administración, Finanzas, Marketing, Operaciones y Recursos Humanos.', 1),
('Modalidades de Graduación', 'Las modalidades de graduación vigentes en la Facultad son: Tesis de Grado, Proyecto de Grado, Examen de Grado y Graduación por Excelencia Académica.', 2),
('Calendario Académico', 'El semestre regular comprende 20 semanas de actividades, evaluaciones parciales, exámenes finales y mesas examinadoras.', 3)
) AS v(titulo, contenido, orden)
WHERE NOT EXISTS (SELECT 1 FROM info_academica);

INSERT INTO materia (sigla, nombre, semestre, creditos, prerrequisito)
SELECT v.sigla, v.nombre, v.semestre, v.creditos, v.prerrequisito FROM (VALUES
('ADM100', 'Introducción a la Administración', 1, 4, 'Ninguno'),
('MAT100', 'Cálculo Aplicado', 1, 4, 'Ninguno'),
('CON100', 'Contabilidad General', 1, 4, 'Ninguno'),
('ADM200', 'Teoría y Procesos Administrativos', 2, 4, 'ADM100'),
('ECO200', 'Microeconomía Aplicada', 2, 4, 'MAT100'),
('FIN300', 'Administración Financiera I', 3, 5, 'CON100'),
('MKT300', 'Fundamentos de Marketing', 3, 4, 'ADM200'),
('RRH400', 'Gestión del Talento Humano', 4, 4, 'ADM200'),
('ADM500', 'Estrategia y Dirección Empresarial', 5, 5, 'FIN300, MKT300')
) AS v(sigla, nombre, semestre, creditos, prerrequisito)
WHERE NOT EXISTS (SELECT 1 FROM materia);

INSERT INTO docente (nombre, titulo, cargo, correo, materia_id)
SELECT v.nombre, v.titulo, v.cargo, v.correo, m.id FROM (VALUES
('Lic. Carlos Morales Méndez', 'Licenciado en Administración de Empresas', 'Docente Titular', 'cmorales@uagrm.edu.bo', 'ADM100'),
('Dra. Patricia Fernández Rojas', 'Doctora en Ciencias Económicas', 'Docente Emérita', 'pfernandez@uagrm.edu.bo', 'FIN300'),
('Mgtr. Roberto Quiroga Arteaga', 'Magíster en Marketing Estratégico', 'Docente Asociado', 'rquiroga@uagrm.edu.bo', 'MKT300'),
('Ing. Marcela Justiniano Vera', 'Magíster en Finanzas Corporativas', 'Docente Titular', 'mjustiniano@uagrm.edu.bo', 'CON100')
) AS v(nombre, titulo, cargo, correo, sigla_mat)
LEFT JOIN materia m ON m.sigla = v.sigla_mat
WHERE NOT EXISTS (SELECT 1 FROM docente);
