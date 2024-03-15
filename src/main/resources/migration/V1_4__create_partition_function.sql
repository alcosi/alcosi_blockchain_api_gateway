/*
 * Copyright (c) 2023 Alcosi Group Ltd. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



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
