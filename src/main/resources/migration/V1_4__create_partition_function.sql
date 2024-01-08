

CREATE OR REPLACE FUNCTION ${schema}.create_partition_by_interval(rq_time_val timestamp,partition_interval_val interval,table_name_val text)
    RETURNS BOOLEAN AS $$
DECLARE
    new_partition_name text;
    already_exist bool;
    period_start timestamp;
    period_end timestamp;
    create_sql text;
BEGIN
    new_partition_name:=split_part(table_name_val, '.', -1)||'_'||
                        date_part('year',rq_time_val)::text||'_'||
                        date_part('month',rq_time_val)::text||'_'||
                        date_part('day',rq_time_val)::text;

    already_exist:=  EXISTS(SELECT FROM pg_class WHERE relname = new_partition_name);
    if already_exist then
        return false;
    END IF;
    period_start:=date_trunc( 'month',rq_time_val);
    period_end:=period_start+ partition_interval_val;
    create_sql:= 'CREATE TABLE '||new_partition_name||' PARTITION OF '||table_name_val||
                 ' FOR VALUES FROM ('''||period_start::text||''') TO ('''||period_end::text||''');';
    raise notice 'Created partition for % %-%',table_name_val,period_start,period_end;
    execute create_sql;
    return true;
END;
$$ LANGUAGE plpgsql;
