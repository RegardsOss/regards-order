package fr.cnes.regards.modules.processing.config;

import fr.cnes.regards.modules.processing.dao.IBatchEntityRepository;
import fr.cnes.regards.modules.processing.dao.IExecutionEntityRepository;
import fr.cnes.regards.modules.processing.dao.PBatchRepositoryImpl;
import fr.cnes.regards.modules.processing.dao.PExecutionRepositoryImpl;
import fr.cnes.regards.modules.processing.entities.BatchEntity;
import fr.cnes.regards.modules.processing.entities.ExecutionEntity;
import fr.cnes.regards.modules.processing.entities.mapping.DaoCustomConverters;
import fr.cnes.regards.modules.processing.entities.mapping.DomainEntityMapperImpl;
import fr.cnes.regards.modules.processing.utils.GsonProcessingUtils;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import name.nkonev.r2dbc.migrate.autoconfigure.R2dbcMigrateAutoConfiguration;
import name.nkonev.r2dbc.migrate.core.Dialect;
import name.nkonev.r2dbc.migrate.core.R2dbcMigrate;
import name.nkonev.r2dbc.migrate.core.R2dbcMigrateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.ReactiveTransactionManager;

import java.util.Collections;

import static io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider.SCHEMA;
import static io.r2dbc.spi.ConnectionFactoryOptions.*;

@Configuration
@EnableR2dbcRepositories(basePackageClasses = {
        IBatchEntityRepository.class,
        IExecutionEntityRepository.class
})
@EntityScan(basePackageClasses = { BatchEntity.class, ExecutionEntity.class })
@ComponentScan(basePackageClasses = {
        DomainEntityMapperImpl.class,
        PBatchRepositoryImpl.class,
        PExecutionRepositoryImpl.class
})
public class R2dbcSpringConfiguration extends AbstractR2dbcConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(R2dbcSpringConfiguration.class);
    @Autowired private final PgSqlConfig pgSqlConfig;

    public R2dbcSpringConfiguration(PgSqlConfig pgSqlConfig) {
        this.pgSqlConfig = pgSqlConfig;
    }

    @Bean public ConnectionFactory connectionFactory() {
        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
               .option(DRIVER, "pool") // This is important to allow large number of parallel calls to db (pooled connections)
               .option(PROTOCOL, "postgresql")
               .option(HOST, pgSqlConfig.getHost())
               .option(PORT, pgSqlConfig.getPort())
               .option(USER, pgSqlConfig.getUser())
               .option(PASSWORD, pgSqlConfig.getPassword())
               .option(DATABASE, pgSqlConfig.getDbname())
               .option(SCHEMA, pgSqlConfig.getSchema())
               .build());
    }

    protected java.util.List<Object> getCustomConverters() {
        return DaoCustomConverters.getCustomConverters(GsonProcessingUtils.gson());
    }

    @Bean(name = "reactiveTransactionManager")
    public ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }


    public static class R2dbcMigrateBlockingInvoker {
        private ConnectionFactory connectionFactory;
        private R2dbcMigrateProperties properties;

        public R2dbcMigrateBlockingInvoker(ConnectionFactory connectionFactory,
                R2dbcMigrateProperties properties) {
            this.connectionFactory = connectionFactory;
            this.properties = properties;
        }

        public void migrate() {
            LOGGER.info("Starting R2DBC migration");
            R2dbcMigrate.migrate(connectionFactory, properties).block();
            LOGGER.info("End of R2DBC migration");
        }

    }

    @Bean
    public R2dbcMigrateProperties r2dbcMigrateProperties() {
        R2dbcMigrateProperties props = new R2dbcMigrateProperties();
        props.setDialect(Dialect.POSTGRESQL);
        props.setResourcesPaths(Collections.singletonList("classpath:/db/migrations/*.sql"));
        return props;
    }

    @Bean(initMethod = "migrate")
    public R2dbcMigrateBlockingInvoker r2dbcMigrate(
            ConnectionFactory connectionFactory,
            R2dbcMigrateProperties properties
    ) {
        return new R2dbcMigrateBlockingInvoker(connectionFactory, properties);
    }

}
