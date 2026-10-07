-- Esquema autoritativo de Educhronos (Bloque 8.5-C2a-DDL).
--
-- Gobierna el esquema en lugar de Hibernate (ddl-auto=none). Quién lo ejecuta, desde
-- S191: PreparadorEsquema, SOLO sobre una base vacía, y la sella con su versión
-- (PRAGMA user_version). Las bases existentes no lo reciben: se migran con
-- esquema/NNN.sql. Los @DataJpaTest siguen pasándolo con el inicializador de Boot
-- (spring.sql.init.mode=always en el application.properties de test). El cuerpo de cada
-- CREATE TABLE es el DDL que Hibernate genera VERBATIM (schema-generation.scripts
-- con el community SQLiteDialect 7.4.1); lo ÚNICO añadido son las 27 FK inline,
-- que el dialecto NO emite (de ahí que hasta 8.5-C1 no hubiera integridad real).
-- Desde las migraciones (S201, S203, S206), las columnas añadidas con ADD COLUMN se
-- escriben aquí tal como las deja SQLite tras la migración, para que una base nueva y
-- una migrada tengan el mismo sqlite_master: grupo_administrativo.aula_referencia_id
-- lleva su FK en la columna, no al final.
--
-- SQLite no soporta ALTER TABLE ADD CONSTRAINT: las FK van DENTRO del CREATE.
-- La integridad requiere ADEMÁS el pragma foreign_keys=ON por conexión, que lo
-- pone un customizer del pool por código (SqliteForeignKeysConfig); sin él estas
-- FK quedan declaradas pero inertes.
--
-- Cascadas (decididas): ON DELETE CASCADE en plaza.actividad_id, en las tres
-- columnas plaza_id de las join tables, en sesion.horario_id, en
-- asignatura_aula_compatible.asignatura_id, en asignatura_aula.asignatura_id y en
-- profesor_tutoria.grupo_id (las tres últimas son población PROPIA de su padre, no
-- referencias entrantes: se van con él). profesor_tutoria.profesor_id NO cascadea: un profesor tutor no se
-- borra en silencio, su borrado da 409. Todo lo demás
-- queda en NO ACTION (equivale a RESTRICT en SQLite), incluidas las autoref
-- nullables grupo_padre_id y siguiente_inmediato_id.
--
-- Idempotencia: sigue siéndolo por "if not exists", NO por demolición, porque los
-- @DataJpaTest lo pasan en cada contexto. Hasta S109 dropeaba las 21 tablas antes de
-- crearlas, lo que vaciaba los datos del usuario en cada arranque de la
-- aplicación (medido: un nivel creado por API desaparecía al reiniciar); por eso
-- se quitaron los DROP.
--
-- Regla de cambio (S191): se edita ESTE fichero, que es lo que reciben las bases
-- nuevas; se añade esquema/NNN.sql con el paso desde la versión anterior, que es lo
-- que reciben las existentes; y se sube PreparadorEsquema.VERSION_ESQUEMA a NNN.
-- esquema/001.sql no se toca nunca: es lo que reciben las bases sin número.

create table if not exists actividad (duracion_tramos integer not null, repeticiones_por_semana integer not null, requiere_tutor boolean not null, asignatura_id bigint, id integer, codigo varchar(255) not null unique, patron_temporal varchar(255) not null check ((patron_temporal in ('DISTRIBUIDA','AGRUPADA','NEUTRA'))), tipo varchar(255) not null default 'CLASE' check ((tipo in ('CLASE','REUNION','FUNCION'))), primary key (id), foreign key (asignatura_id) references asignatura(id));
create table if not exists asignatura (id integer, codigo varchar(255) not null unique, nombre_completo varchar(255) not null, primary key (id));
create table if not exists asignatura_aula (asignatura_id bigint not null, aula_id bigint not null, rol text not null check ((rol in ('EXCLUSIVA','PREFERIDA'))), primary key (asignatura_id, aula_id), foreign key (asignatura_id) references asignatura(id) on delete cascade, foreign key (aula_id) references aula(id));
create table if not exists asignatura_aula_compatible (asignatura_id bigint not null, id integer, tipo_aula varchar(255) not null check ((tipo_aula in ('ORDINARIA','LAB_CIENCIAS','INFORMATICA','TALLER_TEC','TALLER_PLASTICA','GIMNASIO','PISTA','TALLER_FPB','COMUN'))), primary key (id), foreign key (asignatura_id) references asignatura(id) on delete cascade);
create table if not exists aula (capacidad integer, planta integer, id integer, codigo varchar(255) not null unique, edificio varchar(255), sector varchar(255), tipo varchar(255) not null check ((tipo in ('ORDINARIA','LAB_CIENCIAS','INFORMATICA','TALLER_TEC','TALLER_PLASTICA','GIMNASIO','PISTA','TALLER_FPB','COMUN'))), en_uso integer not null default 1, primary key (id));
create table if not exists aula_bloqueada (indice integer not null, actividad_id bigint not null, aula_id bigint not null, id integer, plaza_id bigint not null, primary key (id), foreign key (actividad_id) references actividad(id), foreign key (aula_id) references aula(id), foreign key (plaza_id) references plaza(id));
create table if not exists configuracion (clave varchar(255) not null, valor varchar(255) not null, primary key (clave));
create table if not exists grupo_administrativo (grupo_padre_id bigint, id integer, nivel_id bigint not null, codigo varchar(255) not null unique, tipo varchar(255) not null check ((tipo in ('ORDINARIO','DIVERSIFICACION_PDC','VIRTUAL_OPTATIVA'))), total_declarado integer, aula_referencia_id integer references aula(id), primary key (id), foreign key (grupo_padre_id) references grupo_administrativo(id), foreign key (nivel_id) references nivel(id));
create table if not exists horario_generado (cota_inferior float, objetivo float, fecha_generacion timestamp not null, id integer, estado varchar(255) not null check ((estado in ('BORRADOR','DEFINITIVO','DESCARTADO'))), estado_solver varchar(255) not null, nombre varchar(255) not null, primary key (id));
create table if not exists nivel (orden integer not null, id integer, codigo varchar(255) not null unique, primary key (id));
create table if not exists plaza (actividad_id bigint not null, asignatura_id bigint not null, aula_fija_id bigint, id integer, codigo varchar(255) not null unique, primary key (id), foreign key (actividad_id) references actividad(id) on delete cascade, foreign key (asignatura_id) references asignatura(id), foreign key (aula_fija_id) references aula(id));
create table if not exists plaza_aula_candidata (aula_id bigint not null, plaza_id bigint not null, primary key (aula_id, plaza_id), foreign key (aula_id) references aula(id), foreign key (plaza_id) references plaza(id) on delete cascade);
create table if not exists plaza_profesor (plaza_id bigint not null, profesor_id bigint not null, primary key (plaza_id, profesor_id), foreign key (plaza_id) references plaza(id) on delete cascade, foreign key (profesor_id) references profesor(id));
create table if not exists plaza_subgrupo (plaza_id bigint not null, subgrupo_id bigint not null, primary key (plaza_id, subgrupo_id), foreign key (plaza_id) references plaza(id) on delete cascade, foreign key (subgrupo_id) references subgrupo(id));
create table if not exists profesor (id integer, codigo varchar(255) not null unique, nombre_completo varchar(255) not null, total_declarado integer, cargo varchar(255) not null default 'PROFESOR' check ((cargo in ('PROFESOR','JEFE_ESTUDIOS','DIRECTOR','VICEDIRECTOR','SECRETARIO'))), primary key (id));
create table if not exists profesor_restriccion_horaria (peso integer not null, id integer, profesor_id bigint not null, tramo_id bigint not null, motivo varchar(255), tipo varchar(255) not null check ((tipo in ('DURA','BLANDA'))), primary key (id), foreign key (profesor_id) references profesor(id), foreign key (tramo_id) references tramo_semanal(id));
create table if not exists profesor_tutoria (grupo_id bigint not null, profesor_id bigint not null, rol varchar(255) not null check ((rol in ('TUTOR_PRINCIPAL','CO_TUTOR'))), primary key (profesor_id, grupo_id), foreign key (profesor_id) references profesor(id), foreign key (grupo_id) references grupo_administrativo(id) on delete cascade);
create table if not exists sesion (indice integer not null, aula_id bigint, horario_id bigint not null, id integer, plaza_id bigint not null, tramo_inicio_id bigint not null, primary key (id), foreign key (aula_id) references aula(id), foreign key (horario_id) references horario_generado(id) on delete cascade, foreign key (plaza_id) references plaza(id), foreign key (tramo_inicio_id) references tramo_semanal(id));
create table if not exists sesion_bloqueada (indice integer not null, actividad_id bigint not null, id integer, tramo_inicio_id bigint not null, primary key (id), foreign key (actividad_id) references actividad(id), foreign key (tramo_inicio_id) references tramo_semanal(id));
create table if not exists subgrupo (id integer, codigo varchar(255) not null unique, alumnos integer check (alumnos >= 0), primary key (id));
create table if not exists subgrupo_grupo (grupo_id bigint not null, subgrupo_id bigint not null, primary key (grupo_id, subgrupo_id), foreign key (grupo_id) references grupo_administrativo(id), foreign key (subgrupo_id) references subgrupo(id));
create table if not exists tramo_semanal (es_lectivo boolean not null, hora_fin time(0) not null, hora_inicio time(0) not null, orden integer not null, id integer, siguiente_inmediato_id bigint, dia varchar(255) not null check ((dia in ('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES'))), primary key (id), foreign key (siguiente_inmediato_id) references tramo_semanal(id));

-- curso: identidad del fichero (nombre y si está archivado). FILA ÚNICA: el check (id = 1) se añade a mano,
-- como las FK. NO es configuración: no se copia tal cual al duplicar (O-curso, S159).
create table if not exists curso (id integer not null check (id = 1), nombre varchar(9) not null, archivado boolean not null, primary key (id));

-- Las cuatro únicas que las entidades declaran con @UniqueConstraint (S206, D-unica-declarada-sin-esquema). Hasta el
-- esquema 4 solo las sostenía el código; ahora las tiene la base.
create unique index if not exists uk_sesion_horario_plaza_indice on sesion (horario_id, plaza_id, indice);
create unique index if not exists uk_sesion_bloqueada_actividad_indice on sesion_bloqueada (actividad_id, indice);
create unique index if not exists uk_aula_bloqueada_actividad_indice_plaza on aula_bloqueada (actividad_id, indice, plaza_id);
create unique index if not exists uk_asignatura_aula_compatible_asignatura_tipo on asignatura_aula_compatible (asignatura_id, tipo_aula);
