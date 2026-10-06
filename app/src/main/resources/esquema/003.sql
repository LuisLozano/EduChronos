-- Esquema 3 (S203, C-totales-y-cargo).
-- Total de horas declarado del profesor y del grupo (ordinario o PDC), nulo si no se ha declarado, y cargo del
-- profesor. Tres ADD COLUMN: SQLite coloca cada columna tras la última y antes de las restricciones de tabla, como
-- en schema.sql. Los profesores existentes quedan como PROFESOR y nadie con total.
alter table profesor add column total_declarado integer;
alter table profesor add column cargo varchar(255) not null default 'PROFESOR'
  check ((cargo in ('PROFESOR','JEFE_ESTUDIOS','DIRECTOR','VICEDIRECTOR','SECRETARIO')));
alter table grupo_administrativo add column total_declarado integer;
