-- Esquema 7 (S219, C-dato-guardias-especiales).
-- Guardias de recreo por profesor: cuántas hace cada uno a la semana, como mucho una por recreo. Los mínimos de
-- recreo y de biblioteca viven en configuracion (001.sql) con las claves guardias.minimoPorRecreo y
-- guardias.minimoBibliotecaPorRecreo, y sin fila valen 0. Las bases existentes quedan con 0 guardias de recreo.
alter table profesor add column guardias_recreo integer not null default 0;
