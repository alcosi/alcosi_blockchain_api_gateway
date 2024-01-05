package com.alcosi.nft.apigateway.config.db.r2dbc

import com.alcosi.nft.apigateway.config.path.PathConfigurationProperties
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.pool.PoolingConnectionFactoryProvider
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import java.time.Duration


@Configuration
@EnableConfigurationProperties(R2DBCConnectionFactoryOptionsProperties::class)
@ConditionalOnProperty(prefix = "spring.r2dbc", name = ["url"], matchIfMissing = false)
@Import(value = [R2dbcAutoConfiguration::class, TransactionAutoConfiguration::class, R2dbcTransactionManagerAutoConfiguration::class])
class R2DBCDBConfig{
//    @Bean
//    fun getR2DBCConnectionFactoryOptionsProperties(): R2DBCConnectionFactoryOptionsProperties {
//        return R2DBCConnectionFactoryOptionsProperties()
//    }

    @Bean
    fun getR2DBCConnectionFactoryOptionsBuilderCustomizer(props:R2DBCConnectionFactoryOptionsProperties): R2DBCConnectionFactoryOptionsBuilderCustomizer {
        return R2DBCConnectionFactoryOptionsBuilderCustomizer(props.options.filter { it.value!=null } as Map<String,String>)
    }
    //    @Bean(initMethod = "migrate")
//    fun flyway(
//        flywayProperties: FlywayProperties,
//        r2dbcProperties: R2dbcProperties,
//        @Value("\${spring.r2dbc.url:r2dbc:postgresql://test:5432/test}") dbUrl: String,
//        @Value("\${spring.r2dbc.username:test}") username: String,
//        @Value("\${spring.r2dbc.password:test}") password: String,
//    ): Flyway {
//        return Flyway.configure()
//            .dataSource(
//                flywayProperties.url?:dbUrl,
//                r2dbcProperties.,
//                password
//            )
//            .locations(*flywayProperties.locations.toTypedArray<String>())
//            .baselineOnMigrate(true)
//            .load()
//    }
//    @Bean
    fun databaseClient(@Qualifier("r2bdcConnectionFactory") connectionFactory: ConnectionFactory): DatabaseClient {
        return DatabaseClient.create(connectionFactory)
    }

    //    @Bean("r2bdcConnectionFactory")
    fun connectionFactory(
        r2dbcProperties: R2dbcProperties,
        details: R2dbcConnectionDetails,
        @Value("\${spring.r2dbc.url:r2dbc:pool:postgresql://test:5432/test}") dbUrl: String,
        @Value("\${spring.r2dbc.pool.initial-size:5}") initPoolSize: Int,
        @Value("\${spring.r2dbc.pool.max-size:10}") maxPoolSize: Int,
        @Value("\${spring.r2dbc.pool.retry:2}") retry: Int,
        @Value("\${spring.r2dbc.pool.max-idle-time:10m}") idleTimeout: Duration,
        @Value("\${spring.r2dbc.pool.max-create-connection-time:10s}") maxCreateConnectionTime: Duration,
        @Value("\${spring.r2dbc.pool.max-validation-time:10s}") maxValidationTime: Duration,
        @Value("\${spring.r2dbc.pool.max-acquire-time:10s}") maxAcquireTime: Duration,
        @Value("\${spring.r2dbc.pool.max-life-time:10s}") maxLifeTime: Duration,
        @Value("\${spring.r2dbc.pool.option.lock_timeout:10s}") lockTimeout: String,
        @Value("\${spring.r2dbc.pool.option.statement_timeout:5m}") statementTimeout: String,
    ): ConnectionFactory {
        details.connectionFactoryOptions
        r2dbcProperties.properties["schema"]
        val options: MutableMap<String, String> = mutableMapOf()
        options["lock_timeout"] = lockTimeout
        options["statement_timeout"] = statementTimeout
        val option = ConnectionFactoryBuilder
            .withUrl(r2dbcProperties.url)
            .password(r2dbcProperties.password)
            .username(r2dbcProperties.username)
            .buildOptions()
            .mutate()
            .option(PostgresqlConnectionFactoryProvider.OPTIONS, options)
            .option(ConnectionFactoryOptions.DRIVER, PoolingConnectionFactoryProvider.POOLING_DRIVER)
            .option(
                ConnectionFactoryOptions.PROTOCOL,
                "${PoolingConnectionFactoryProvider.POOLING_DRIVER}:${PostgresqlConnectionFactoryProvider.POSTGRESQL_DRIVER}"
            )
        val builder = ConnectionFactoryBuilder.withOptions(option)
        val connectionFactory = builder.build()
        val configuration = ConnectionPoolConfiguration.builder(connectionFactory)
            .initialSize(initPoolSize)
            .maxSize(maxPoolSize)
            .maxIdleTime(idleTimeout)
            .acquireRetry(retry)
            .maxValidationTime(maxValidationTime)
            .maxAcquireTime(maxAcquireTime)
            .maxLifeTime(maxLifeTime)
            .maxCreateConnectionTime(maxCreateConnectionTime)
            .validationQuery("Select 1")
            .build()
        return ConnectionPool(configuration)
    }

    companion object {
        val SQL_NAME_LIMITATIONS_REGEX = Regex("^(?!pg_)[a-z][a-z0-9).]{0,32}\$")

        @JvmStatic
        fun r2dbcURLToJdbcURL(value: String?): String? {
            return value?.replace(":pool", "")?.replace("r2dbc", "jdbc")
        }
    }
}
