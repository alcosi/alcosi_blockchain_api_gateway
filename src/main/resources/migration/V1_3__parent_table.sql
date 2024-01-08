create sequence if not exists ${schema}.api_gateway_request_history_id;


CREATE TABLE IF NOT EXISTS ${schema}.api_gateway_request_history
(
    id             bigint default nextval('${schema}.api_gateway_request_history_id'),
    rq_headers     jsonb,
    ip             text,
    uri            text,
    method         ${schema}.http_method_type,
    auth_details jsonb,
    user_id        uuid,
    account_id     uuid,
    matched_route_details  jsonb,
    routed_service text,
    rq_type        text,
    rq_size        bigint,
    rs_size        bigint,
    rs_code        int,
    rq_time        timestamp not null,
    rs_time        timestamp,
    rs_delay       interval generated always as ((rq_time - rs_time) ) stored
) PARTITION BY RANGE (rq_time);


