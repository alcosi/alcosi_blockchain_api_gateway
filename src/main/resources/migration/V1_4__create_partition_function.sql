

CREATE OR REPLACE FUNCTION ${schema}.create_partition_by_interval(rq_time_val timestamp,partition_interval_val interval,table_name_val text,scheme_name_val text)
    RETURNS BOOLEAN AS $$
DECLARE
    new_partition_name text;
    already_exist bool;
    period_start timestamp;
    period_end timestamp;
    create_partition_sql text;
    create_rq_id_index_sql text;
    create_id_index_sql text;
BEGIN
    new_partition_name:=table_name_val||'_'||
                        date_part('year',rq_time_val)::text||'_'||
                        date_part('month',rq_time_val)::text||'_'||
                        date_part('day',rq_time_val)::text;

    already_exist:=  EXISTS(SELECT FROM pg_class WHERE relname = new_partition_name);
    if already_exist then
        return false;
    END IF;
    period_start:=date_trunc( 'month',rq_time_val);
    period_end:=period_start+ partition_interval_val;
    create_partition_sql:= 'CREATE TABLE '||scheme_name_val||'.'||new_partition_name||' PARTITION OF '||scheme_name_val||'.'||table_name_val||
                 ' FOR VALUES FROM ('''||period_start::text||''') TO ('''||period_end::text||''');';
    raise notice 'Created partition for % %-%',table_name_val,period_start,period_end;
    execute create_partition_sql;
    create_rq_id_index_sql:='create index if not exists '||new_partition_name||'_rq_id_index on '||scheme_name_val||'.'||new_partition_name ||' (rq_id)';
    execute create_rq_id_index_sql;
    create_id_index_sql:='create unique index if not exists '||new_partition_name||'_id_index on '||scheme_name_val||'.'||new_partition_name ||' (id)';
    execute create_rq_id_index_sql;
    return true;
END;
$$ LANGUAGE plpgsql;

