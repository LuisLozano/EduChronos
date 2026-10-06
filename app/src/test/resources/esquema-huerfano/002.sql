-- Juego huérfano de PreparadorEsquemaTest: de la versión 1 a la 2 deja una hija cuyo padre no existe. Con las
-- claves foráneas apagadas SQLite la acepta; la guarda del preparador tiene que deshacer la migración entera.
insert into hija (id, padre_id) values (1, 99);
