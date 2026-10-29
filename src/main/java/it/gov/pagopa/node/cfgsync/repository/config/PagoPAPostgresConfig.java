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
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "it.gov.pagopa.node.cfgsync.repository.pagopa",
        entityManagerFactoryRef = "pagopaPostgresEntityManagerFactory",
        transactionManagerRef = "pagopaPostgresTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.pagopa.postgres", name = "enabled", havingValue = "true")
public class PagoPAPostgresConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.pagopa.postgres")
    public DataSourceProperties pagopaPostgresDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.pagopa.postgres")
    public DataSource pagopaPostgresDataSource() {
        HikariDataSource build = pagopaPostgresDatasourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
        build.setPoolName("pagopaPostgres");
        return build;
    }

    @Primary
    @Bean(name = "pagopaPostgresEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean pagopaPostgresEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(pagopaPostgresDataSource())
                .packages(ConfigCache.class)
                .build();
    }

    @Primary
    @Bean(name = "pagopaPostgresTransactionManager")
    public PlatformTransactionManager pagopaPostgresTransactionManager(
            final @Qualifier("pagopaPostgresEntityManagerFactory") LocalContainerEntityManagerFactoryBean pagopaPostgresEntityManagerFactory) {
        return new JpaTransactionManager(pagopaPostgresEntityManagerFactory.getObject());
    }

    @Primary
    @Bean(name = "pagopaPostgresTransactionTemplate")
    public TransactionTemplate pagopaPostgresTransactionTemplate(
            final @Qualifier("pagopaPostgresTransactionManager") PlatformTransactionManager pagopaPostgresTransactionManager) {
        return new TransactionTemplate(pagopaPostgresTransactionManager);
    }

}
