create sequence if not exists ${schema}.api_gateway_request_history_id;


CREATE TABLE IF NOT EXISTS ${schema}.api_gateway_request_history
(
    id             bigint default nextval('${schema}.api_gateway_request_history_id'),
    rq_id          text,
    rq_headers     jsonb,
    ip             text,
    uri            text,
    method         ${schema}.http_method_type,
    auth_details jsonb,
    user_id        uuid,
    account_id     uuid,
    matched_route_details  jsonb,
    routed_service text,
    rq_size        bigint,
    rs_size        bigint,
    rs_code        int,
    rq_time        timestamp not null,
    rs_time        timestamp,
    rs_delay       interval generated always as ((rs_time - rq_time) ) stored,
    primary key (id,rq_time)
) PARTITION BY RANGE (rq_time);

create index if not exists api_gateway_request_history_rq_id_index on  ${schema}.api_gateway_request_history(rq_id);
create index if not exists api_gateway_request_history_id_index on  ${schema}.api_gateway_request_history(id);
