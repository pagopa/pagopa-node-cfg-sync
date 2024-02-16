package it.gov.pagopa.node.cfgsync.repository.configuration;

import com.zaxxer.hikari.HikariDataSource;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.repository.model.StandInStations;
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
@PropertySources({
        @PropertySource("classpath:/application.properties"),
        @PropertySource(value = "classpath:/application-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
})
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "it.gov.pagopa.node.cfgsync.repository.pagopa.standin",
        entityManagerFactoryRef = "pagoPAStandInPostgreEntityManagerFactory",
        transactionManagerRef = "pagoPAStandInPostgreTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.pagopa.postgre.standin", name = "enabled")
public class PagoPAStandInPostgreConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource.pagopa.postgre.standin")
    public DataSourceProperties pagoPAStandInPostgreDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.pagopa.postgre.standin.configuration")
    public DataSource pagoPAStandInPostgreDataSource() {
        return pagoPAStandInPostgreDatasourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = "pagoPAStandInPostgreEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean pagoPAStandInPostgreEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(pagoPAStandInPostgreDataSource())
                .packages(StandInStations.class)
                .build();
    }

    @Bean
    public PlatformTransactionManager pagoPAStandInPostgreTransactionManager(
            final @Qualifier("pagoPAStandInPostgreEntityManagerFactory") LocalContainerEntityManagerFactoryBean pagoPAStandInPostgreEntityManagerFactory) {
        return new JpaTransactionManager(pagoPAStandInPostgreEntityManagerFactory.getObject());
    }

}
