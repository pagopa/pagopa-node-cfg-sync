package it.gov.pagopa.node.cfgsync.repository.config;

import com.zaxxer.hikari.HikariDataSource;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "it.gov.pagopa.node.cfgsync.repository.nexipostgres",
        entityManagerFactoryRef = "nexiPostgresEntityManagerFactory",
        transactionManagerRef = "nexiPostgresTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.nexi.postgres", name = "enabled")
public class NexiPostgresConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.postgres")
    public DataSourceProperties nexiPostgresDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.postgres")
    public DataSource nexiPostgresDataSource() {
        HikariDataSource build = nexiPostgresDatasourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
        build.setPoolName("nexiPostgres");
        return build;
    }

    @Bean(name = "nexiPostgresEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean nexiPostgresEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(nexiPostgresDataSource())
                .packages(ConfigCache.class)
                .build();
    }

    @Bean(name = "nexiPostgresTransactionManager")
    public PlatformTransactionManager nexiPostgresTransactionManager(
            final @Qualifier("nexiPostgresEntityManagerFactory") LocalContainerEntityManagerFactoryBean nexiPostgresEntityManagerFactory) {
        return new JpaTransactionManager(nexiPostgresEntityManagerFactory.getObject());
    }

}
