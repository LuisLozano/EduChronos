-- Esquema 5 (S212, C-dato-guardias).
-- Guardias ordinarias semanales de cada profesor, un número entero que no baja de 0. Un ADD COLUMN: SQLite coloca la
-- columna tras la última y antes de las restricciones de tabla, como en schema.sql. Los profesores existentes quedan
-- con 0 guardias. El mínimo de profesores de guardia por tramo del centro no necesita esquema: vive en configuracion
-- (001.sql) con la clave guardias.minimoPorTramo, y sin fila vale el de ConfiguracionGuardias.
alter table profesor add column guardias_ordinarias integer not null default 0;
