-- Juego huérfano de PreparadorEsquemaTest (S201): versión 1, una tabla padre y una hija que la referencia.
create table if not exists padre (id integer primary key);
create table if not exists hija (id integer primary key, padre_id integer references padre(id));
