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
