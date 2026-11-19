# Portal Web de la Carrera de Administración de Empresas (UAGRM) — Sistema vía E-Mail

**Proyecto 1 · Tecnología Web (INF513-SA) · Grupo 17 SA · Gestión 2026-2**

Integrantes:
- Mamani Peña Daniel Joaquin
- Ponce Yepez Andres Reyes
- Lozada Arias Victor Adolfo

Sistema con arquitectura *Email-Driven*: el usuario escribe un comando en el **asunto** de un correo y lo envía a
`grupo17sa@tecnoweb.org.bo`. El sistema lee el buzón por **POP3**, valida el comando, lo ejecuta en **PostgreSQL**
con sentencias preparadas y responde al remitente por **SMTP** con «Éxito» y los datos, o con «Error» y el detalle.

```
mail from : estudiante@gmail.com
rcpt to   : grupo17sa@tecnoweb.org.bo
subject   : LISBAN["*"]
```

## Formato de los comandos

`COMANDO["parámetro 1","parámetro 2",...]`

- El nombre del comando puede ir en mayúsculas o minúsculas; se ignoran los espacios de más.
- Cada parámetro va entre comillas dobles. Dentro de un parámetro, `\"` escribe una comilla y `\\` una barra.
- Se exige la **cantidad exacta** de parámetros. Los opcionales se envían vacíos: `""`.
- El tipo de cada parámetro (entero, enlace, correo, fecha, largo máximo...) se valida antes de llegar a la base de datos.
- Un comando sin parámetros se escribe `INICIO` o `INICIO[]`.
- Los comandos `LIS...` reciben un filtro: `"*"` lista todo y otro texto busca coincidencias.
- `HELP` envía la lista completa de comandos; `HELP["CU2"]` solo los de un caso de uso.

## Arquitectura (3 capas)

```
src/main/java/bo/tecnoweb/grupo17sa/
├── App.java                 Punto de entrada y modos de ejecución
├── Configuracion.java       Lee config.properties
├── Registro.java            logs/access.log y logs/error.log
├── presentacion/            CAPA DE PRESENTACIÓN
│   ├── correo/              LectorCorreo (POP3), EmisorCorreo (SMTP), HiloCiclico (demonio), CodificacionMIME
│   ├── lexico/              AnalizadorLexico: asunto → Comando
│   ├── comandos/            Despachador, definición y validación de parámetros, ComandosCUx (uno por caso de uso)
│   └── vista/               Respuesta, PlantillaHTML, TablaHTML, Parrafo
├── negocio/                 CAPA DE NEGOCIO: reglas de cada caso de uso, transacciones, bitácora y permisos
├── datos/                   CAPA DE DATOS: ConexionDB y un DAO por tabla (solo PreparedStatement)
└── modelo/                  Un record por tabla
src/main/resources/sql/      Scripts de la base de datos (uno por caso de uso)
```

- El correo se maneja **solo con sockets**, con el mismo esquema de `ClientePOP` y `ClienteSMTP` de la Lección 3.0
  (`USER/PASS/STAT/RETR/DELE/QUIT` y `EHLO/MAIL FROM/RCPT TO/DATA/QUIT`). Si el puerto 25 está bloqueado, el
  cliente SMTP también admite STARTTLS o SSL con `AUTH LOGIN` (por ejemplo, Gmail con una contraseña de aplicación).
- Cada operación que modifica datos se ejecuta en una **transacción** junto con su registro en la bitácora.
- La respuesta es un correo HTML (UTF-8): un recuadro verde de «Éxito» o rojo de «Error», y las tablas con los datos.
  Puede llevar archivos adjuntos (`multipart/mixed`, por ejemplo la exportación CSV de graduados) y, además de la
  respuesta al remitente, el sistema puede enviar **avisos** a otra persona (por ejemplo, al solicitante de INNOVA).

## Diagrama de flujo del analizador de correos

```mermaid
flowchart TD
    A[HiloCiclico: cada N segundos] --> B[LectorCorreo: POP3 USER, PASS, STAT, RETR, DELE]
    B --> C{¿Es una respuesta automática?}
    C -- Sí --> Z[Se ignora y se anota en access.log]
    C -- No --> D[AnalizadorLexico: asunto → nombre + parámetros]
    D -- Sintaxis inválida --> E[Error: Comando mal escrito]
    D -- Sintaxis válida --> F{¿Existe el comando?}
    F -- No --> G[Error: Comando desconocido]
    F -- Sí --> H{¿Cantidad y tipos correctos?}
    H -- No --> I[Error: Parámetros incorrectos]
    H -- Sí --> J{¿Es público o el remitente tiene permiso?}
    J -- No --> K[Error: Permiso denegado]
    J -- Sí --> L[Negocio y DAO: transacción con PreparedStatement]
    L -- Error de negocio o de SQL --> M[Error con el detalle del fallo]
    L -- Correcto --> N[Éxito con los datos o el resumen]
    E & G & I & K & M & N --> O[PlantillaHTML y EmisorCorreo: SMTP al remitente]
    O --> P[access.log y error.log]
```

## Base de datos

Base `db_grupo17sa` (PostgreSQL). Scripts en `src/main/resources/sql/`; se instalan con `--instalar-bd`.

### CU1 Gestión de Usuarios, roles y permisos (`cu1_usuarios.sql`)

| Tabla | Columnas | Uso |
|-------|----------|-----|
| `rol` | **id**, nombre (único, en mayúsculas), descripcion, creado | Roles del sistema (RF-18) |
| `rol_modulo` | **rol_id**, **modulo** | Casos de uso que administra cada rol: `CU1` a `CU8`, o `*` para todos (RF-18) |
| `usuario` | **id**, correo (único, en minúsculas), nombre, rol_id, activo, creado, actualizado | Usuarios: se identifican por el correo desde el que envían los comandos (RF-18) |

Roles iniciales: `ADMINISTRADOR` (`*`), `EDITOR_INSTITUCIONAL` (CU2, CU4, CU5), `EDITOR_COMUNICACION` (CU7) y `CONSULTOR` (CU8).

### CU2 Portal Web y Bitácora de Auditoría (`cu2_portal.sql`)

| Tabla | Columnas | Uso |
|-------|----------|-----|
| `configuracion` | **clave**, valor, grupo, descripcion, actualizado | Bienvenida (RF-01) y datos de contacto (RF-16) |
| `banner` | **id**, titulo, subtitulo, imagen_url, enlace, orden, creado | Carrusel de la página de inicio (RF-01) |
| `destacado` | **id**, titulo, descripcion, enlace, orden, creado | Contenidos destacados (RF-01) |
| `acceso_rapido` | **id**, nombre, url, descripcion, orden | Accesos a plataformas de la Universidad (RF-01) |
| `seccion_institucional` | **clave**, titulo, contenido, orden, actualizado | Historia, misión, visión, etc. (RF-02) |
| `red_social` | **id**, nombre (único), url, orden | Redes sociales (RF-16) |
| `bitacora` | **id**, fecha, usuario, accion, modulo, tabla, elemento, detalle | Acciones de los administradores en todos los casos de uso (RF-19) |

### CU4 Investigaciones, Revista INNOVAR y Repositorio Documental (`cu4_investigacion.sql`)

| Tabla | Columnas | Uso |
|-------|----------|-----|
| `investigacion_linea` | **id**, nombre (único), descripcion, creado | Líneas de investigación (RF-07) |
| `investigacion_proyecto` | **id**, titulo, tipo, linea_id, responsable, integrantes, gestion, estado, resumen, publicado, creado, actualizado | Proyectos de investigación, de aula (PIA) y de semillero (RF-07) |
| `documento` | **id**, titulo, descripcion, categoria, enlace, publicado, creado, actualizado | Repositorio de documentos con el enlace al archivo (RF-06) |
| `revista_edicion` | **id**, volumen, numero, anio, titulo, descripcion, enlace_pdf, publicada, creado | Ediciones de la revista INNOVAR; (anio, numero) es único (RF-08) |
| `revista_articulo` | **id**, edicion_id, titulo, autores, resumen, enlace_pdf, orden, creado | Artículos de cada edición; se borran con su edición (RF-08) |

Los archivos (PDF) se publican en el servidor web de la materia y en la base se guarda su enlace. Una edición nueva queda
en borrador hasta publicarla; lo oculto y los borradores solo los ve quien administra el CU4.

### CU5 Vinculación, Empresa Junior INNOVA y Alumni (`cu5_vinculacion.sql`)

| Tabla | Columnas | Uso |
|-------|----------|-----|
| `convenio` | **id**, tipo, institucion, objeto, fecha_inicio, fecha_fin, estado, responsable, logo_url, creado | Convenios, prácticas, proyectos, consultorías e interacción social (RF-09) |
| `innova_servicio` | **id**, nombre, descripcion | Servicios de INNOVA (RF-10) |
| `innova_miembro` | **id**, nombre, cargo, semestre, correo | Equipo de INNOVA (RF-10) |
| `innova_proyecto` | **id**, nombre, cliente, descripcion, anio, resultado | Proyectos realizados por INNOVA (RF-10) |
| `innova_solicitud` | **id**, tipo, nombre, correo, telefono, organizacion, mensaje, estado, observacion, fecha, atendido_por, atendido_en | Solicitudes de empresas y postulaciones de estudiantes (RF-10) |
| `graduado` | **id**, correo (único), nombre, apellidos, telefono, anio_titulacion, empleo_actual, cargo, sector, ciudad, registrado, actualizado | Seguimiento de graduados (RF-11) |

Los valores permitidos de `tipo`, `estado` y `sector` están en restricciones `CHECK` y se validan antes de llegar a la base.
La descripción de INNOVA se guarda en `configuracion` (clave `innova_descripcion`).

### CU7 Formulario de Contacto y Búsqueda Interna Global (`cu7_contacto.sql`)

| Tabla | Columnas | Uso |
|-------|----------|-----|
| `mensaje_contacto` | **id**, correo, nombre, telefono, asunto, mensaje, estado, respuesta, respondido_por, respondido_en, fecha | Mensajes enviados a la Carrera (RF-15) |

`estado`: NUEVO, LEIDO, RESPONDIDO o ARCHIVADO. La búsqueda global (RF-17) no tiene tablas propias: consulta el contenido
publicado de los demás casos de uso (`BusquedaDAO.FUENTES`).

## Comandos implementados

### Permisos (CU1)

Un comando «Con permiso» lo puede usar un usuario **activo** cuyo rol tiene ese caso de uso (o `*`). Los correos de
`admin.correos` en `config.properties` son **administradores de arranque**: tienen permiso aunque no estén en la base y
sirven para registrar el primer usuario. Nadie puede desactivarse ni eliminarse a sí mismo, y el sistema no permite
quedarse sin un administrador activo cuando no hay administradores de arranque.

### CU1 Gestión de Usuarios, roles y permisos

| Comando | Acceso | Descripción |
|---------|--------|-------------|
| `LISUSU["*"]` · `INSUSU["correo","nombre","rol"]` · `MODUSU["id","nombre","rol","activo"]` · `ELIUSU["id"]` | Con permiso | Usuarios (activo: SI o NO) |
| `LISROL["*"]` · `INSROL["nombre","descripcion","casos_de_uso"]` · `MODROL["id",...]` · `ELIROL["id"]` | Con permiso | Roles; casos_de_uso: `CU2,CU4` o `*`. El rol ADMINISTRADOR no se borra ni pierde el `*` |
| `MISROL` | Público | Usuario, rol y casos de uso del remitente |

Ejemplos:

```
INSUSU["director@ejemplo.edu.bo","Director de Carrera","ADMINISTRADOR"]
INSROL["editor de prensa","Noticias y mensajes","CU2, CU7"]
MODUSU["2","Editora Institucional","CONSULTOR","SI"]
```

### CU2 Portal Web y Bitácora de Auditoría

| Comando | Acceso | Descripción |
|---------|--------|-------------|
| `INICIO` | Público | Bienvenida, banners, contenidos destacados y accesos rápidos |
| `LISBAN["*"]` · `INSBAN["titulo","subtitulo","imagen_url","enlace","orden"]` · `MODBAN["id",...]` · `ELIBAN["id"]` | Con permiso | Banners del carrusel |
| `LISDES["*"]` · `INSDES["titulo","descripcion","enlace","orden"]` · `MODDES["id",...]` · `ELIDES["id"]` | Con permiso | Contenidos destacados |
| `LISACC["*"]` · `INSACC["nombre","url","descripcion","orden"]` · `MODACC["id",...]` · `ELIACC["id"]` | Con permiso | Accesos rápidos |
| `LISINT["*"]` · `VERINT["mision"]` | Público | Información institucional |
| `MODINT["clave","titulo","contenido"]` | Con permiso | Modifica una sección institucional |
| `CONTACTO` | Público | Dirección, mapa, teléfonos, correos, horario y redes sociales |
| `LISCFG["*"]` · `MODCFG["clave","valor"]` | Con permiso | Datos configurables del portal |
| `LISRED["*"]` · `INSRED["nombre","url","orden"]` · `MODRED["id",...]` · `ELIRED["id"]` | Con permiso | Redes sociales |
| `LISBIT["*"]` · `LISBIT["correo o texto"]` · `LISBIT["2026-11-20"]` | Con permiso | Bitácora de auditoría |

Ejemplos:

```
INSBAN["Feria de emprendimiento","Del 25 al 27 de noviembre","https://www.tecnoweb.org.bo/inf513/grupo17sa/img/feria.jpg","","4"]
MODINT["mision","Misión","Formar profesionales en Administración de Empresas..."]
INSRED["TikTok","https://www.tiktok.com/","4"]
```

### CU4 Investigaciones, Revista INNOVAR y Repositorio Documental

Los comandos públicos muestran solo lo publicado; quien administra el CU4 ve también lo oculto y los borradores.

| Comando | Acceso | Descripción |
|---------|--------|-------------|
| `LISLIN["*"]` | Público | Líneas de investigación con su cantidad de proyectos publicados |
| `INSLIN["nombre","descripcion"]` · `MODLIN["id",...]` · `ELILIN["id"]` | Con permiso | Una línea con proyectos no se puede eliminar |
| `LISPRO["*"]` · `VERPRO["id"]` | Público | Proyectos de investigación (filtro por título, responsable, línea o tipo) |
| `INSPRO["titulo","tipo","linea_id","responsable","integrantes","gestion","estado","resumen"]` · `MODPRO["id",...]` · `PUBPRO["id","SI/NO"]` · `ELIPRO["id"]` | Con permiso | tipo: INVESTIGACION, AULA o SEMILLERO; estado: EN_CURSO o CONCLUIDO; linea_id vacío si no tiene |
| `LISREP["*"]` | Público | Repositorio de documentos con el enlace a cada archivo |
| `INSREP["titulo","descripcion","categoria","enlace"]` · `MODREP["id",...]` · `PUBREP["id","SI/NO"]` · `ELIREP["id"]` | Con permiso | categoría: REGLAMENTO, RESOLUCION, FORMULARIO, PLAN_DE_ESTUDIOS, INFORME u OTRO |
| `LISREV["*"]` · `VERREV["id"]` | Público | Ediciones de la revista INNOVAR y sus artículos |
| `INSREV["volumen","numero","anio","titulo","descripcion","enlace_pdf"]` · `MODREV["id",...]` · `PUBREV["id","SI/NO"]` · `ELIREV["id"]` | Con permiso | La edición nueva queda en borrador; eliminarla borra sus artículos |
| `INSART["edicion","titulo","autores","resumen","enlace_pdf"]` · `MODART["id",...]` · `ELIART["id"]` | Con permiso | Artículos de una edición |

Ejemplos:

```
INSPRO["Talento humano en mypes cruceñas","investigacion","3","Dra. Rosa Quispe","Luis Pérez; Ana Gómez","2026","en curso","Estudio sobre la gestión del talento humano en las mypes de Santa Cruz."]
INSREP["Reglamento de titulación","Modalidades y requisitos","reglamento","https://www.tecnoweb.org.bo/inf513/grupo17sa/docs/titulacion.pdf"]
INSREV["2","7","2026","Número especial","Innovación y gestión en la región",""]
```

### CU5 Vinculación, Empresa Junior INNOVA y Alumni

| Comando | Acceso | Descripción |
|---------|--------|-------------|
| `LISCNV["*"]` | Público | Convenios y actividades de vinculación (filtro por tipo, institución o estado) |
| `INSCNV["tipo","institucion","objeto","fecha_inicio","fecha_fin","estado","responsable","logo_url"]` · `MODCNV["id",...]` · `ELICNV["id"]` | Con permiso | tipo: CONVENIO, PRACTICA, PROYECTO, CONSULTORIA o INTERACCION; estado: VIGENTE, EN_TRAMITE o CONCLUIDO |
| `INNOVA` | Público | Descripción, servicios, equipo y proyectos de INNOVA |
| `MODINN["descripcion"]` | Con permiso | Cambia la descripción de INNOVA |
| `LISSRV` · `INSSRV["nombre","descripcion"]` · `MODSRV` · `ELISRV` | Con permiso | Servicios de INNOVA |
| `LISMIE` · `INSMIE["nombre","cargo","semestre","correo"]` · `MODMIE` · `ELIMIE` | Con permiso | Equipo de INNOVA |
| `LISPRY` · `INSPRY["nombre","cliente","descripcion","anio","resultado"]` · `MODPRY` · `ELIPRY` | Con permiso | Proyectos realizados por INNOVA |
| `SOLINN["tipo","nombre","telefono","organizacion","mensaje"]` | Público | Solicitud de servicio (SERVICIO) o postulación (POSTULACION); se guarda con el correo del remitente (máximo 3 pendientes por correo) |
| `MISSOL` | Público | Estado de las solicitudes del remitente |
| `LISSOL["PENDIENTE"]` · `ATESOL["id","ATENDIDA o RECHAZADA","observacion"]` | Con permiso | Atiende una solicitud y **envía un aviso por correo** al solicitante |
| `REGGRA["nombre","apellidos","telefono","anio_titulacion","empleo_actual","cargo","sector","ciudad"]` · `ACTGRA[...]` · `MISGRA` | Público | El graduado se registra, actualiza y consulta sus propios datos (siempre con su correo) |
| `LISGRA["*"]` · `EXPGRA["*"]` · `ELIGRA["id"]` | Con permiso | Consulta, **exporta a CSV adjunto** (se abre en Excel) y elimina graduados |

Ejemplos:

```
SOLINN["SERVICIO","Ferretería Demo","70011111","Comercio","Necesitamos un estudio de mercado"]
REGGRA["Carla","Mendoza Rivas","72000000","2021","Banco Demo S.A.","Analista","PRIVADO","Santa Cruz"]
ATESOL["3","ATENDIDA","Un consultor de INNOVA le llamará esta semana."]
```

Las opciones aceptan minúsculas y tildes: `"práctica"` se guarda como `PRACTICA` y `"en trámite"` como `EN_TRAMITE`.

### CU7 Formulario de Contacto y Búsqueda Interna Global

| Comando | Acceso | Descripción |
|---------|--------|-------------|
| `ENVMSJ["nombre","telefono","asunto","mensaje"]` | Público | Mensaje a la Carrera; queda con el correo del remitente (máximo 3 sin responder por correo) y **avisa por correo** a quienes administran el CU7 |
| `MISMSJ` | Público | Estado y respuesta de los mensajes del remitente |
| `LISMSJ["*"]` · `VERMSJ["id"]` | Con permiso | Mensajes (filtro: un estado o un texto); VERMSJ lo marca como LEIDO |
| `RESMSJ["id","respuesta"]` | Con permiso | Responde y **envía la respuesta por correo** al remitente |
| `ARCMSJ["id"]` · `ELIMSJ["id"]` | Con permiso | Archiva o elimina un mensaje |
| `BUSCAR["texto"]` | Público | Busca en el contenido publicado de todos los casos de uso, sin distinguir mayúsculas ni tildes |

Ejemplos:

```
ENVMSJ["Ana Pérez","70011122","Consulta de admisión","Quisiera saber las fechas de inscripción."]
RESMSJ["1","Las inscripciones abren el 2 de enero en la Dirección de Carrera."]
BUSCAR["reglamento de titulación"]
```

### CU8 Reportes y Estadísticas

Todos necesitan permiso sobre el CU8. Las cantidades se muestran con barras hechas en HTML (sin imágenes).

| Comando | Descripción |
|---------|-------------|
| `REPGEN` | Cantidad de registros de cada caso de uso (los no instalados aparecen como «no instalado») |
| `REPBIT["desde","hasta"]` | Acciones de la bitácora entre dos fechas, por usuario, caso de uso y acción |
| `REPVIN` | Convenios por tipo y estado, y los vigentes que vencen en los próximos 60 días o ya vencieron |
| `REPALU` | Graduados por sector, año de titulación y ciudad, y porcentaje con empleo |
| `REPINN` | Solicitudes a INNOVA por estado y tipo, y tiempo promedio de atención |
| `REPUSO["desde","hasta"]` | Uso del sistema según `logs/access.log`: correos procesados, éxitos, errores, tiempo de respuesta, comandos más usados y remitentes |

El CU8 no tiene tablas propias: consulta las de los demás casos de uso (`ReporteDAO`). Para que el resumen general
incluya otro caso de uso, se agregan sus tablas en `ReporteDAO.TABLAS_RESUMEN`.

## Instalación y configuración

Requisitos: Java 17 o superior, Maven 3.9 y PostgreSQL.

```bash
cp config.properties.ejemplo config.properties   # completar las contraseñas (este archivo no se sube a GitHub)
mvn clean package                                # compila, ejecuta las pruebas y genera target/portal-correo.jar
java -jar target/portal-correo.jar --instalar-bd # crea las tablas y carga los datos iniciales
java -jar target/portal-correo.jar --probar-correo   # prueba POP3 y SMTP sin enviar nada
java -jar target/portal-correo.jar               # inicia el servicio (Ctrl+C para detenerlo)
```

Otras opciones:

| Opción | Uso |
|--------|-----|
| `--una-vez` | Revisa el buzón una sola vez y termina |
| `--probar "LISBAN[\"*\"]" [--de correo@x.com]` | Ejecuta un comando sin correo; guarda el correo de respuesta en `salida/` |
| `--consola [--de correo@x.com]` | Escribe comandos uno por uno para probarlos |
| `--enviar "INICIO" --a correo@x.com [--de correo@y.com]` | Ejecuta un comando y envía la respuesta por SMTP, sin leer el buzón |
| `--config ruta` | Usa otro archivo de configuración |

En `config.properties`:
- `db.*`: conexión a PostgreSQL (`localhost` para desarrollo o `db.tecnoweb.org.bo` en el servidor).
- `pop3.*`: buzón `grupo17sa` en `mail.tecnoweb.org.bo:110`.
- `smtp.*`: `mail.tecnoweb.org.bo:25` sin autenticación, o Gmail (`smtp.gmail.com:587`, `starttls`).
- `admin.correos`: administradores de arranque (separados por comas); con ellos se registra el primer usuario con `INSUSU`.

Solo debe haber **una** instancia leyendo el buzón: los correos se borran del servidor al procesarlos.

## Logs

- `logs/access.log`: cada correo procesado, el comando, el resultado (ÉXITO/ERROR) y el tiempo.
- `logs/error.log`: cada excepción capturada con su traza (fallos de conexión, SQL, POP3 o SMTP).

## Cómo agregar un caso de uso

1. `src/main/resources/sql/cuX_nombre.sql` con sus tablas (`CREATE TABLE IF NOT EXISTS`) y agregarlo a `InstaladorBD.SCRIPTS`.
2. `modelo/`: un `record` por tabla.
3. `datos/`: un DAO por tabla, solo con `PreparedStatement`.
4. `negocio/`: las reglas del caso de uso. Cada alta, cambio o baja se hace dentro de `Transaccion.ejecutar(...)` y
   llama a `BitacoraNegocio.registrar(...)` con la misma conexión.
5. `presentacion/comandos/ComandosCUx.java`: implementa `ModuloComandos` (definición de comandos con sus parámetros y
   el `switch` que llama al negocio) y se agrega en la lista `modulos` del `Despachador`. Las respuestas estándar
   («Registro insertado correctamente», listados...) están en `Respuestas`.

Si dos casos de uso usan el mismo nombre de comando, el programa no arranca y dice cuál está repetido.
Nombres ya usados: `HELP`; CU1: `USU`, `ROL`, `MISROL`; CU4: `LIN`, `PRO`, `DOC`, `REV`, `ART` (con LIS/INS/MOD/ELI/VER/PUB);
CU7: `MSJ` (con ENV/MIS/LIS/VER/RES/ARC/ELI), `BUSCAR`; CU2: `INICIO`, `CONTACTO`, `BAN`, `DES`, `ACC`, `INT`, `CFG`, `RED`, `BIT`;
CU5: `CNV`, `INNOVA`, `INN`, `SRV`, `MIE`, `PRY`, `SOLINN`, `SOL`, `GRA` (con los prefijos LIS/INS/MOD/ELI/VER/MIS/REG/ACT/EXP/ATE);
CU8: `REPGEN`, `REPBIT`, `REPVIN`, `REPALU`, `REPINN`, `REPUSO`.
