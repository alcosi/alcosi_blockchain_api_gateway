

create or replace function ${schema}.create_partition_by_interval(rq_time_val timestamp without time zone, partition_interval_val interval, table_name_val text, scheme_name_val text) returns boolean
    language plpgsql
as $$
DECLARE
    new_partition_name text;
    already_exist bool;
    period_start timestamp;
    period_end timestamp;
    create_partition_sql text;
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
    execute create_partition_sql;
    raise notice 'Created partition for % %-%',table_name_val,period_start,period_end;
    return true;
END;
$$;
