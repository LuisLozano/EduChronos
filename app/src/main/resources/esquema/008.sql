-- Esquema 8 (S220, C-reparto-equilibrado).
-- Tipo de cada guardia repartida: ORDINARIA en un tramo de clase, RECREO o BIBLIOTECA en un recreo. Un ADD COLUMN:
-- SQLite coloca la columna tras la última y antes de las restricciones de tabla, como en schema.sql. Las guardias
-- existentes quedan como ORDINARIA, que es lo que eran; la única (horario, profesor, tramo) no cambia.
alter table guardia add column tipo varchar(255) not null default 'ORDINARIA' check ((tipo in ('ORDINARIA','RECREO','BIBLIOTECA')));
