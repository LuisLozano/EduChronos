-- Esquema 9 (S250, C-asignatura-con-nivel, O-actividad-por-nivel).
-- Cada asignatura tiene un nivel o ninguno. La que lo tiene lleva como código el corto, un guion y el código del
-- nivel (Biol-3ESO), y el corto se guarda aparte. Dos ADD COLUMN: SQLite coloca cada columna tras la última y antes
-- de las restricciones de tabla, como en schema.sql, y la FK del nivel va en la propia columna porque ADD COLUMN no
-- admite restricciones de tabla.
--
-- Una asignatura usada en varios niveles se parte en una por nivel. Nivel de una plaza: el de los grupos de sus
-- subgrupos si es uno solo; con varios la plaza está mezclada, y sin subgrupos no tiene nivel. Partes de una
-- asignatura: una por cada nivel de sus plazas no mezcladas con nivel. La principal, que conserva el id de la fila
-- original, es la del nivel con más de esas plazas; en empate, la de menor nivel.orden y luego la de menor
-- nivel.id. Una asignatura sin partes (sin plazas, o solo con plazas sin subgrupos) queda sin nivel y con su
-- código. Nivel de una actividad: la unión de los niveles de sus plazas no mezcladas.
--
-- Cada sentencia repite la consulta del nivel de las plazas en su WITH: no hay más tabla temporal que la de la
-- comprobación de choques, y las plazas no cambian de subgrupos en toda la migración.
alter table asignatura add column codigo_corto varchar(255);
alter table asignatura add column nivel_id integer references nivel(id);

-- (1) El código corto es el código de antes en todas las filas.
update asignatura set codigo_corto = codigo;

-- (2) Choques, ANTES de escribir nada más: un compuesto que iguala un código existente o que se repite entre los
-- compuestos. Si el recuento no es 0, el check de la tabla temporal hace fallar esta sentencia, que es la que lleva
-- el nombre de la tabla, y el preparador deshace la migración entera: la base se queda en la 8.
create temp table codigo_con_nivel_repetido (n integer check (n = 0));
with nivel_de_plaza as (
        select ps.plaza_id, min(g.nivel_id) as nivel_id
        from plaza_subgrupo ps
        join subgrupo_grupo sg on sg.subgrupo_id = ps.subgrupo_id
        join grupo_administrativo g on g.id = sg.grupo_id
        group by ps.plaza_id
        having count(distinct g.nivel_id) = 1),
    partes as (
        select distinct p.asignatura_id, np.nivel_id
        from plaza p
        join nivel_de_plaza np on np.plaza_id = p.id),
    compuestos as (
        select a.codigo_corto || '-' || n.codigo as codigo
        from partes pa
        join asignatura a on a.id = pa.asignatura_id
        join nivel n on n.id = pa.nivel_id)
insert into codigo_con_nivel_repetido (n)
select (select count(*) from compuestos c where c.codigo in (select codigo from asignatura))
     + (select count(*) from (select codigo from compuestos group by codigo having count(*) > 1));

-- (3) La fila original pasa a ser la parte principal.
with nivel_de_plaza as (
        select ps.plaza_id, min(g.nivel_id) as nivel_id
        from plaza_subgrupo ps
        join subgrupo_grupo sg on sg.subgrupo_id = ps.subgrupo_id
        join grupo_administrativo g on g.id = sg.grupo_id
        group by ps.plaza_id
        having count(distinct g.nivel_id) = 1),
    cuenta as (
        select p.asignatura_id, np.nivel_id, count(*) as plazas
        from plaza p
        join nivel_de_plaza np on np.plaza_id = p.id
        group by p.asignatura_id, np.nivel_id),
    puesto as (
        select c.asignatura_id, c.nivel_id,
               row_number() over (partition by c.asignatura_id order by c.plazas desc, n.orden, n.id) as puesto
        from cuenta c
        join nivel n on n.id = c.nivel_id)
update asignatura set nivel_id = (
    select pu.nivel_id from puesto pu where pu.asignatura_id = asignatura.id and pu.puesto = 1);

-- (4) Una fila nueva por cada otra parte, con el mismo corto y el mismo nombre, y el código ya compuesto.
with nivel_de_plaza as (
        select ps.plaza_id, min(g.nivel_id) as nivel_id
        from plaza_subgrupo ps
        join subgrupo_grupo sg on sg.subgrupo_id = ps.subgrupo_id
        join grupo_administrativo g on g.id = sg.grupo_id
        group by ps.plaza_id
        having count(distinct g.nivel_id) = 1),
    partes as (
        select distinct p.asignatura_id, np.nivel_id
        from plaza p
        join nivel_de_plaza np on np.plaza_id = p.id)
insert into asignatura (codigo, nombre_completo, codigo_corto, nivel_id)
select a.codigo_corto || '-' || n.codigo, a.nombre_completo, a.codigo_corto, pa.nivel_id
from partes pa
join asignatura a on a.id = pa.asignatura_id
join nivel n on n.id = pa.nivel_id
where pa.nivel_id <> a.nivel_id
order by a.id, n.orden, n.id;

-- (5) Cada plaza no mezclada con nivel pasa a la parte de su nivel: la fila con su mismo corto y ese nivel. Las
-- mezcladas y las que no tienen nivel se quedan en la principal.
with nivel_de_plaza as (
        select ps.plaza_id, min(g.nivel_id) as nivel_id
        from plaza_subgrupo ps
        join subgrupo_grupo sg on sg.subgrupo_id = ps.subgrupo_id
        join grupo_administrativo g on g.id = sg.grupo_id
        group by ps.plaza_id
        having count(distinct g.nivel_id) = 1)
update plaza set asignatura_id = (
    select b.id
    from asignatura a
    join asignatura b on b.codigo_corto = a.codigo_corto
    join nivel_de_plaza np on np.nivel_id = b.nivel_id
    where a.id = plaza.asignatura_id and np.plaza_id = plaza.id)
where id in (select plaza_id from nivel_de_plaza);

-- (6) Una actividad de un solo nivel cuya asignatura tiene parte de ese nivel pasa a esa parte; si no, se queda
-- donde está.
with nivel_de_plaza as (
        select ps.plaza_id, min(g.nivel_id) as nivel_id
        from plaza_subgrupo ps
        join subgrupo_grupo sg on sg.subgrupo_id = ps.subgrupo_id
        join grupo_administrativo g on g.id = sg.grupo_id
        group by ps.plaza_id
        having count(distinct g.nivel_id) = 1),
    nivel_de_actividad as (
        select p.actividad_id, min(np.nivel_id) as nivel_id
        from plaza p
        join nivel_de_plaza np on np.plaza_id = p.id
        group by p.actividad_id
        having count(distinct np.nivel_id) = 1),
    destino as (
        select na.actividad_id, b.id as asignatura_id
        from nivel_de_actividad na
        join actividad ac on ac.id = na.actividad_id
        join asignatura a on a.id = ac.asignatura_id
        join asignatura b on b.codigo_corto = a.codigo_corto and b.nivel_id = na.nivel_id)
update actividad set asignatura_id = (
    select d.asignatura_id from destino d where d.actividad_id = actividad.id)
where id in (select actividad_id from destino);

-- (7) Las reglas de aula de la original, copiadas a cada parte nueva. Hasta el paso (8) la original es la fila cuyo
-- código es su corto, y la nueva, la que ya lo lleva compuesto.
insert into asignatura_aula (asignatura_id, aula_id, rol)
select b.id, aa.aula_id, aa.rol
from asignatura b
join asignatura a on a.codigo_corto = b.codigo_corto and a.codigo = a.codigo_corto
join asignatura_aula aa on aa.asignatura_id = a.id
where b.codigo <> b.codigo_corto;
insert into asignatura_aula_compatible (asignatura_id, tipo_aula)
select b.id, ac.tipo_aula
from asignatura b
join asignatura a on a.codigo_corto = b.codigo_corto and a.codigo = a.codigo_corto
join asignatura_aula_compatible ac on ac.asignatura_id = a.id
where b.codigo <> b.codigo_corto;

-- (8) Las originales con nivel ganan el código compuesto. Las que no tienen nivel conservan el suyo.
update asignatura set codigo = codigo_corto || '-' || (select n.codigo from nivel n where n.id = asignatura.nivel_id)
where nivel_id is not null and codigo = codigo_corto;

-- (9)
drop table codigo_con_nivel_repetido;
