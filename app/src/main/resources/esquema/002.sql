-- Esquema 2 (S201, C-actividad-sin-alumnos).
-- sesion.aula_id deja de ser obligatoria: una reunión o una función puede no tener aula.
-- Reconstrucción dentro de la transacción del preparador: ninguna tabla apunta a sesion, así que no hace falta
-- apagar las claves foráneas. Se renombra PRIMERO la vieja: el orden inverso deja el nombre entrecomillado en
-- sqlite_master y la base migrada ya no coincidiría con una nueva (medido en S201, F2).
alter table sesion rename to sesion_v1;
create table if not exists sesion (indice integer not null, aula_id bigint, horario_id bigint not null, id integer, plaza_id bigint not null, tramo_inicio_id bigint not null, primary key (id), foreign key (aula_id) references aula(id), foreign key (horario_id) references horario_generado(id) on delete cascade, foreign key (plaza_id) references plaza(id), foreign key (tramo_inicio_id) references tramo_semanal(id));
insert into sesion (indice, aula_id, horario_id, id, plaza_id, tramo_inicio_id) select indice, aula_id, horario_id, id, plaza_id, tramo_inicio_id from sesion_v1;
drop table sesion_v1;
-- Tipo de actividad: las existentes quedan como CLASE.
alter table actividad add column tipo varchar(255) not null default 'CLASE' check ((tipo in ('CLASE','REUNION','FUNCION')));
