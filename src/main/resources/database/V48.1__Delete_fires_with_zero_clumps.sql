delete from fire f
where f.id in (
    select f.id
    from fire f
    left join clump c on c.fire_id = f.id
    group by f.id
    having count(c.id) = 0
)