-- Esquema 4 (S206, C-reglas-aulas): las reglas de aulas del centro y las cuatro únicas que el código declaraba
-- y la base no tenía (D-unica-declarada-sin-esquema).
-- Tres ADD COLUMN: SQLite coloca cada columna tras la última y antes de las restricciones de tabla, como en
-- schema.sql. La FK del aula de referencia va en la propia columna porque ADD COLUMN no admite restricciones de
-- tabla. Los grupos existentes quedan sin aula de referencia, los subgrupos sin alumnos y todas las aulas en uso.
alter table grupo_administrativo add column aula_referencia_id integer references aula(id);
alter table subgrupo add column alumnos integer check (alumnos >= 0);
alter table aula add column en_uso integer not null default 1;
-- Aulas de cada asignatura: exclusivas o preferidas, nunca las dos en la misma asignatura (lo valida el servicio).
create table if not exists asignatura_aula (asignatura_id bigint not null, aula_id bigint not null, rol text not null check ((rol in ('EXCLUSIVA','PREFERIDA'))), primary key (asignatura_id, aula_id), foreign key (asignatura_id) references asignatura(id) on delete cascade, foreign key (aula_id) references aula(id));
-- Las cuatro únicas declaradas en las entidades (@UniqueConstraint), con sus columnas exactas.
create unique index if not exists uk_sesion_horario_plaza_indice on sesion (horario_id, plaza_id, indice);
create unique index if not exists uk_sesion_bloqueada_actividad_indice on sesion_bloqueada (actividad_id, indice);
create unique index if not exists uk_aula_bloqueada_actividad_indice_plaza on aula_bloqueada (actividad_id, indice, plaza_id);
create unique index if not exists uk_asignatura_aula_compatible_asignatura_tipo on asignatura_aula_compatible (asignatura_id, tipo_aula);
