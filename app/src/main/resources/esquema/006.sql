-- Esquema 6 (S213, C-reparto-guardias).
-- Guardias ordinarias repartidas en un horario generado: una fila por profesor y tramo. Se van con su horario
-- (on delete cascade, como sesion.horario_id). Profesor y tramo quedan en NO ACTION, como las demás FK de sesion,
-- y sus borrados los guardan ProfesorService y JornadaService con un 409. La única impide dos guardias del mismo
-- profesor en el mismo tramo de un horario. Las bases existentes quedan sin ninguna guardia.
create table if not exists guardia (horario_id bigint not null, id integer, profesor_id bigint not null, tramo_id bigint not null, primary key (id), foreign key (horario_id) references horario_generado(id) on delete cascade, foreign key (profesor_id) references profesor(id), foreign key (tramo_id) references tramo_semanal(id));
create unique index if not exists uk_guardia_horario_profesor_tramo on guardia (horario_id, profesor_id, tramo_id);
