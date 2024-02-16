package it.gov.pagopa.node.cfgsync.repository.configuration;

import com.zaxxer.hikari.HikariDataSource;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import it.gov.pagopa.node.cfgsync.repository.model.StandInStations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
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
        basePackages = "it.gov.pagopa.node.cfgsync.repository.nexioracle.standin",
        entityManagerFactoryRef = "nexiStandInOracleEntityManagerFactory",
        transactionManagerRef = "nexiStandInOracleTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.nexi.oracle.standin", name = "enabled")
public class NexiStandInOracleConfiguration {

    @Autowired
    private Environment env;

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.oracle.standin")
    public DataSourceProperties nexiStandInOracleDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.oracle.standin.configuration")
    public DataSource nexiStandInOracleDataSource() {
        return nexiStandInOracleDatasourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = "nexiStandInOracleEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean nexiStandInOracleEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(nexiStandInOracleDataSource())
                .packages(StandInStations.class)
                .build();
    }

    @Bean
    public PlatformTransactionManager nexiStandInOracleTransactionManager(
            final @Qualifier("nexiStandInOracleEntityManagerFactory") LocalContainerEntityManagerFactoryBean nexiStandInOracleEntityManagerFactory) {
        return new JpaTransactionManager(nexiStandInOracleEntityManagerFactory.getObject());
    }
}