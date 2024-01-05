package com.alcosi.nft.apigateway.service.statistic

import com.alcosi.nft.apigateway.config.db.r2dbc.R2DBCDBConfig.Companion.SQL_NAME_LIMITATIONS_REGEX
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.reactive.TransactionalOperator

open class StatisticDBInitializer(
    val databaseClient: DatabaseClient,
    val transactionalOperator: TransactionalOperator,
    val schemaName:String = "request_history"
) {
    protected open val createSchemaSql = """
create schema if not exists ${schemaName};
    """
   protected open val createHttpMethodEnumSql = """
    DO
    ${'$'}${'$'}
        BEGIN
            create type ${schemaName}.http_method_type as enum ('GET','HEAD','POST','PUT','DELETE','CONNECT','OPTIONS','TRACE','PATCH');
        EXCEPTION
            WHEN duplicate_object THEN null;
        END
    ${'$'}${'$'};
    """
    init {
        if (!SQL_NAME_LIMITATIONS_REGEX.matches(schemaName)){
            throw IllegalArgumentException("Wrong schema name $schemaName")
        }
    }

    fun initTable(){
        makeBlockedSqlInTrx(createHttpMethodEnumSql)

        databaseClient
            .sql(
                """
                    CREATE TABLE IF NOT EXISTS $schemaName.api_gateway_request_history ( 
                    id bigserial primary key,
                    rq_headers hstore,
                    ip text,
                    uri text,
                    method http_method_type,
                    user_id uuid,
                    account_id uuid,
                    matched_route text,
                    routed_service text,
                    rq_type text,
                    rq_size bigint,
                    rs_size bigint,
                    rs_code int,
                    rq_time timestamp not null ,
                    rq_date date generated always as ( (rq_time::date) ) stored ,
                    rs_time timestamp  ,
                    rs_delay interval generated always as ((rq_time-rq_date)  ) stored,
                    );""").fetch()
            .rowsUpdated().block()
    }

    protected open fun makeBlockedSqlInTrx(sql:String) {
        transactionalOperator.transactional(
        databaseClient
            .sql(sql).fetch().rowsUpdated()).block()
    }
}