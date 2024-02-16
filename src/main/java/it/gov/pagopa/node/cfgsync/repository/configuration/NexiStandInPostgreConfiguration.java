package it.gov.pagopa.node.cfgsync.repository.configuration;

import com.zaxxer.hikari.HikariDataSource;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.repository.model.StandInStations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
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
        basePackages = "it.gov.pagopa.node.cfgsync.repository.nexipostgre.standin",
        entityManagerFactoryRef = "nexiStandInPostgreEntityManagerFactory",
        transactionManagerRef = "nexiStandInPostgreTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.nexi.postgre.standin", name = "enabled")
public class NexiStandInPostgreConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.postgre.standin")
    public DataSourceProperties nexiStandInPostgreDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.postgre.standin.configuration")
    public DataSource nexiStandInPostgreDataSource() {
        return nexiStandInPostgreDatasourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = "nexiStandInPostgreEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean nexiStandInPostgreEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(nexiStandInPostgreDataSource())
                .packages(StandInStations.class)
                .build();
    }

    @Bean
    public PlatformTransactionManager nexiStandInPostgreTransactionManager(
            final @Qualifier("nexiStandInPostgreEntityManagerFactory") LocalContainerEntityManagerFactoryBean nexiStandInPostgreEntityManagerFactory) {
        return new JpaTransactionManager(nexiStandInPostgreEntityManagerFactory.getObject());
    }

}
