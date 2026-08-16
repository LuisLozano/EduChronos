-- Esquema autoritativo de Educhronos (Bloque 8.5-C2a-DDL).
--
-- Gobierna el esquema en lugar de Hibernate (ddl-auto=none). Lo ejecuta Spring
-- (spring.sql.init.mode=always) en CADA arranque de contexto. El cuerpo de cada
-- CREATE TABLE es el DDL que Hibernate genera VERBATIM (schema-generation.scripts
-- con el community SQLiteDialect 7.4.1); lo ÚNICO añadido son las 27 FK inline,
-- que el dialecto NO emite (de ahí que hasta 8.5-C1 no hubiera integridad real).
--
-- SQLite no soporta ALTER TABLE ADD CONSTRAINT: las FK van DENTRO del CREATE.
-- La integridad requiere ADEMÁS el pragma foreign_keys=ON por conexión, que lo
-- pone un customizer del pool por código (SqliteForeignKeysConfig); sin él estas
-- FK quedan declaradas pero inertes.
--
-- Cascadas (decididas): ON DELETE CASCADE en plaza.actividad_id, en las tres
-- columnas plaza_id de las join tables, en sesion.horario_id, en
-- asignatura_aula_compatible.asignatura_id y en profesor_tutoria.grupo_id (las
-- dos últimas son población PROPIA de su padre, no referencias entrantes: se van
-- con él). profesor_tutoria.profesor_id NO cascadea: un profesor tutor no se
-- borra en silencio, su borrado da 409. Todo lo demás
-- queda en NO ACTION (equivale a RESTRICT en SQLite), incluidas las autoref
-- nullables grupo_padre_id y siguiente_inmediato_id.
--
-- Idempotencia: este fichero se ejecuta en CADA arranque, y es idempotente por
-- "if not exists", NO por demolición. Hasta S109 dropeaba las 21 tablas antes de
-- crearlas, lo que vaciaba los datos del usuario en cada arranque de la
-- aplicación (medido: un nivel creado por API desaparecía al reiniciar); por eso
-- se quitaron los DROP.
--
-- Consecuencia: un cambio de esquema NO se aplica solo sobre una BD ya existente
-- (el CREATE se salta la tabla que ya está). En desarrollo, para adoptar un
-- cambio hay que borrar el .db y dejar que se recree. La migración de verdad,
-- sobre datos reales, es asunto de H4 "pasar de curso".

create table if not exists actividad (duracion_tramos integer not null, repeticiones_por_semana integer not null, requiere_tutor boolean not null, asignatura_id bigint, id integer, codigo varchar(255) not null unique, patron_temporal varchar(255) not null check ((patron_temporal in ('DISTRIBUIDA','AGRUPADA','NEUTRA'))), primary key (id), foreign key (asignatura_id) references asignatura(id));
create table if not exists asignatura (id integer, codigo varchar(255) not null unique, nombre_completo varchar(255) not null, primary key (id));
create table if not exists asignatura_aula_compatible (asignatura_id bigint not null, id integer, tipo_aula varchar(255) not null check ((tipo_aula in ('ORDINARIA','LAB_CIENCIAS','INFORMATICA','TALLER_TEC','TALLER_PLASTICA','GIMNASIO','PISTA','TALLER_FPB','COMUN'))), primary key (id), foreign key (asignatura_id) references asignatura(id) on delete cascade);
create table if not exists aula (capacidad integer, planta integer, id integer, codigo varchar(255) not null unique, edificio varchar(255), sector varchar(255), tipo varchar(255) not null check ((tipo in ('ORDINARIA','LAB_CIENCIAS','INFORMATICA','TALLER_TEC','TALLER_PLASTICA','GIMNASIO','PISTA','TALLER_FPB','COMUN'))), primary key (id));
create table if not exists aula_bloqueada (indice integer not null, actividad_id bigint not null, aula_id bigint not null, id integer, plaza_id bigint not null, primary key (id), foreign key (actividad_id) references actividad(id), foreign key (aula_id) references aula(id), foreign key (plaza_id) references plaza(id));
create table if not exists configuracion (clave varchar(255) not null, valor varchar(255) not null, primary key (clave));
create table if not exists grupo_administrativo (grupo_padre_id bigint, id integer, nivel_id bigint not null, codigo varchar(255) not null unique, tipo varchar(255) not null check ((tipo in ('ORDINARIO','DIVERSIFICACION_PDC','VIRTUAL_OPTATIVA'))), primary key (id), foreign key (grupo_padre_id) references grupo_administrativo(id), foreign key (nivel_id) references nivel(id));
create table if not exists horario_generado (cota_inferior float, objetivo float, fecha_generacion timestamp not null, id integer, estado varchar(255) not null check ((estado in ('BORRADOR','DEFINITIVO','DESCARTADO'))), estado_solver varchar(255) not null, nombre varchar(255) not null, primary key (id));
create table if not exists nivel (orden integer not null, id integer, codigo varchar(255) not null unique, primary key (id));
create table if not exists plaza (actividad_id bigint not null, asignatura_id bigint not null, aula_fija_id bigint, id integer, codigo varchar(255) not null unique, primary key (id), foreign key (actividad_id) references actividad(id) on delete cascade, foreign key (asignatura_id) references asignatura(id), foreign key (aula_fija_id) references aula(id));
create table if not exists plaza_aula_candidata (aula_id bigint not null, plaza_id bigint not null, primary key (aula_id, plaza_id), foreign key (aula_id) references aula(id), foreign key (plaza_id) references plaza(id) on delete cascade);
create table if not exists plaza_profesor (plaza_id bigint not null, profesor_id bigint not null, primary key (plaza_id, profesor_id), foreign key (plaza_id) references plaza(id) on delete cascade, foreign key (profesor_id) references profesor(id));
create table if not exists plaza_subgrupo (plaza_id bigint not null, subgrupo_id bigint not null, primary key (plaza_id, subgrupo_id), foreign key (plaza_id) references plaza(id) on delete cascade, foreign key (subgrupo_id) references subgrupo(id));
create table if not exists profesor (id integer, codigo varchar(255) not null unique, nombre_completo varchar(255) not null, primary key (id));
create table if not exists profesor_restriccion_horaria (peso integer not null, id integer, profesor_id bigint not null, tramo_id bigint not null, motivo varchar(255), tipo varchar(255) not null check ((tipo in ('DURA','BLANDA'))), primary key (id), foreign key (profesor_id) references profesor(id), foreign key (tramo_id) references tramo_semanal(id));
create table if not exists profesor_tutoria (grupo_id bigint not null, profesor_id bigint not null, rol varchar(255) not null check ((rol in ('TUTOR_PRINCIPAL','CO_TUTOR'))), primary key (profesor_id, grupo_id), foreign key (profesor_id) references profesor(id), foreign key (grupo_id) references grupo_administrativo(id) on delete cascade);
create table if not exists sesion (indice integer not null, aula_id bigint not null, horario_id bigint not null, id integer, plaza_id bigint not null, tramo_inicio_id bigint not null, primary key (id), foreign key (aula_id) references aula(id), foreign key (horario_id) references horario_generado(id) on delete cascade, foreign key (plaza_id) references plaza(id), foreign key (tramo_inicio_id) references tramo_semanal(id));
create table if not exists sesion_bloqueada (indice integer not null, actividad_id bigint not null, id integer, tramo_inicio_id bigint not null, primary key (id), foreign key (actividad_id) references actividad(id), foreign key (tramo_inicio_id) references tramo_semanal(id));
create table if not exists subgrupo (id integer, codigo varchar(255) not null unique, primary key (id));
create table if not exists subgrupo_grupo (grupo_id bigint not null, subgrupo_id bigint not null, primary key (grupo_id, subgrupo_id), foreign key (grupo_id) references grupo_administrativo(id), foreign key (subgrupo_id) references subgrupo(id));
create table if not exists tramo_semanal (es_lectivo boolean not null, hora_fin time(0) not null, hora_inicio time(0) not null, orden integer not null, id integer, siguiente_inmediato_id bigint, dia varchar(255) not null check ((dia in ('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES'))), primary key (id), foreign key (siguiente_inmediato_id) references tramo_semanal(id));
