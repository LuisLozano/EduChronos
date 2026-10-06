-- Juego huérfano de PreparadorEsquemaTest: el esquema vigente es 001 + 002 en una sola definición.
create table if not exists padre (id integer primary key);
create table if not exists hija (id integer primary key, padre_id integer references padre(id));
insert into hija (id, padre_id) values (1, 99);
