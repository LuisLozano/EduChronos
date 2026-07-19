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
-- Idempotencia: se DROPEA todo antes de crear (orden hijo->padre para no violar
-- FK durante el borrado), de modo que varios contextos Spring sobre el mismo
-- fichero recrean el esquema sin petar.

drop table if exists sesion;
drop table if exists aula_bloqueada;
drop table if exists sesion_bloqueada;
drop table if exists profesor_restriccion_horaria;
drop table if exists profesor_tutoria;
drop table if exists plaza_aula_candidata;
drop table if exists plaza_profesor;
drop table if exists plaza_subgrupo;
drop table if exists subgrupo_grupo;
drop table if exists asignatura_aula_compatible;
drop table if exists plaza;
drop table if exists horario_generado;
drop table if exists actividad;
drop table if exists subgrupo;
drop table if exists grupo_administrativo;
drop table if exists asignatura;
drop table if exists aula;
drop table if exists nivel;
drop table if exists profesor;
drop table if exists tramo_semanal;
drop table if exists configuracion;

create table actividad (duracion_tramos integer not null, repeticiones_por_semana integer not null, requiere_tutor boolean not null, asignatura_id bigint, id integer, codigo varchar(255) not null unique, patron_temporal varchar(255) not null check ((patron_temporal in ('DISTRIBUIDA','AGRUPADA','NEUTRA'))), primary key (id), foreign key (asignatura_id) references asignatura(id));
create table asignatura (id integer, codigo varchar(255) not null unique, nombre_completo varchar(255) not null, primary key (id));
create table asignatura_aula_compatible (asignatura_id bigint not null, id integer, tipo_aula varchar(255) not null check ((tipo_aula in ('ORDINARIA','LAB_CIENCIAS','INFORMATICA','TALLER_TEC','TALLER_PLASTICA','GIMNASIO','PISTA','TALLER_FPB','COMUN'))), primary key (id), foreign key (asignatura_id) references asignatura(id) on delete cascade);
create table aula (capacidad integer, planta integer, id integer, codigo varchar(255) not null unique, edificio varchar(255), sector varchar(255), tipo varchar(255) not null check ((tipo in ('ORDINARIA','LAB_CIENCIAS','INFORMATICA','TALLER_TEC','TALLER_PLASTICA','GIMNASIO','PISTA','TALLER_FPB','COMUN'))), primary key (id));
create table aula_bloqueada (indice integer not null, actividad_id bigint not null, aula_id bigint not null, id integer, plaza_id bigint not null, primary key (id), foreign key (actividad_id) references actividad(id), foreign key (aula_id) references aula(id), foreign key (plaza_id) references plaza(id));
create table configuracion (clave varchar(255) not null, valor varchar(255) not null, primary key (clave));
create table grupo_administrativo (grupo_padre_id bigint, id integer, nivel_id bigint not null, codigo varchar(255) not null unique, tipo varchar(255) not null check ((tipo in ('ORDINARIO','DIVERSIFICACION_PDC','VIRTUAL_OPTATIVA'))), primary key (id), foreign key (grupo_padre_id) references grupo_administrativo(id), foreign key (nivel_id) references nivel(id));
create table horario_generado (cota_inferior float, objetivo float, fecha_generacion timestamp not null, id integer, estado varchar(255) not null check ((estado in ('BORRADOR','DEFINITIVO','DESCARTADO'))), estado_solver varchar(255) not null, nombre varchar(255) not null, primary key (id));
create table nivel (orden integer not null, id integer, codigo varchar(255) not null unique, primary key (id));
create table plaza (actividad_id bigint not null, asignatura_id bigint not null, aula_fija_id bigint, id integer, codigo varchar(255) not null unique, primary key (id), foreign key (actividad_id) references actividad(id) on delete cascade, foreign key (asignatura_id) references asignatura(id), foreign key (aula_fija_id) references aula(id));
create table plaza_aula_candidata (aula_id bigint not null, plaza_id bigint not null, primary key (aula_id, plaza_id), foreign key (aula_id) references aula(id), foreign key (plaza_id) references plaza(id) on delete cascade);
create table plaza_profesor (plaza_id bigint not null, profesor_id bigint not null, primary key (plaza_id, profesor_id), foreign key (plaza_id) references plaza(id) on delete cascade, foreign key (profesor_id) references profesor(id));
create table plaza_subgrupo (plaza_id bigint not null, subgrupo_id bigint not null, primary key (plaza_id, subgrupo_id), foreign key (plaza_id) references plaza(id) on delete cascade, foreign key (subgrupo_id) references subgrupo(id));
create table profesor (id integer, codigo varchar(255) not null unique, nombre_completo varchar(255) not null, primary key (id));
create table profesor_restriccion_horaria (peso integer not null, id integer, profesor_id bigint not null, tramo_id bigint not null, motivo varchar(255), tipo varchar(255) not null check ((tipo in ('DURA','BLANDA'))), primary key (id), foreign key (profesor_id) references profesor(id), foreign key (tramo_id) references tramo_semanal(id));
create table profesor_tutoria (grupo_id bigint not null, profesor_id bigint not null, rol varchar(255) not null check ((rol in ('TUTOR_PRINCIPAL','CO_TUTOR'))), primary key (profesor_id, grupo_id), foreign key (profesor_id) references profesor(id), foreign key (grupo_id) references grupo_administrativo(id) on delete cascade);
create table sesion (indice integer not null, aula_id bigint not null, horario_id bigint not null, id integer, plaza_id bigint not null, tramo_inicio_id bigint not null, primary key (id), foreign key (aula_id) references aula(id), foreign key (horario_id) references horario_generado(id) on delete cascade, foreign key (plaza_id) references plaza(id), foreign key (tramo_inicio_id) references tramo_semanal(id));
create table sesion_bloqueada (indice integer not null, actividad_id bigint not null, id integer, tramo_inicio_id bigint not null, primary key (id), foreign key (actividad_id) references actividad(id), foreign key (tramo_inicio_id) references tramo_semanal(id));
create table subgrupo (id integer, codigo varchar(255) not null unique, primary key (id));
create table subgrupo_grupo (grupo_id bigint not null, subgrupo_id bigint not null, primary key (grupo_id, subgrupo_id), foreign key (grupo_id) references grupo_administrativo(id), foreign key (subgrupo_id) references subgrupo(id));
create table tramo_semanal (es_lectivo boolean not null, hora_fin time(0) not null, hora_inicio time(0) not null, orden integer not null, id integer, siguiente_inmediato_id bigint, dia varchar(255) not null check ((dia in ('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES'))), primary key (id), foreign key (siguiente_inmediato_id) references tramo_semanal(id));
