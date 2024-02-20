package it.gov.pagopa.node.cfgsync.repository.configuration;

import com.zaxxer.hikari.HikariDataSource;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
//@PropertySources({
//        @PropertySource("classpath:/application.properties"),
//        @PropertySource(value = "classpath:/application-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
//})
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "it.gov.pagopa.node.cfgsync.repository.pagopa",
        entityManagerFactoryRef = "pagoPAPostgresEntityManagerFactory",
        transactionManagerRef = "pagoPAPostgresTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.pagopa.postgres", name = "enabled")
public class PagoPAPostgresConfiguration {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.pagopa.postgres")
    public DataSourceProperties pagoPAPostgresDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.pagopa.postgres")
    public DataSource pagoPAPostgresDataSource() {
        return pagoPAPostgresDatasourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Primary
    @Bean(name = "pagoPAPostgresEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean pagoPAPostgresEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(pagoPAPostgresDataSource())
                .packages(ConfigCache.class)
                .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager pagoPAPostgresTransactionManager(
            final @Qualifier("pagoPAPostgresEntityManagerFactory") LocalContainerEntityManagerFactoryBean pagoPAPostgresEntityManagerFactory) {
        return new JpaTransactionManager(pagoPAPostgresEntityManagerFactory.getObject());
    }

}
