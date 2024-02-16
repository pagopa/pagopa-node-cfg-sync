package it.gov.pagopa.node.cfgsync.repository.config;

import com.zaxxer.hikari.HikariDataSource;
import it.gov.pagopa.node.cfgsync.repository.model.ConfigCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
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
        basePackages = "it.gov.pagopa.node.cfgsync.repository.nexioracle",
        entityManagerFactoryRef = "nexiOracleEntityManagerFactory",
        transactionManagerRef = "nexiOracleTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.nexi.oracle", name = "enabled")
public class NexiOracleConfiguration {

    @Autowired
    private Environment env;

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.oracle")
    public DataSourceProperties nexiOracleDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.oracle.configuration")
    public DataSource nexiOracleDataSource() {
        return nexiOracleDatasourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = "nexiOracleEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean nexiOracleEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(nexiOracleDataSource())
                .packages(ConfigCache.class)
                .build();
    }

    @Bean
    public PlatformTransactionManager nexiOracleTransactionManager(
            final @Qualifier("nexiOracleEntityManagerFactory") LocalContainerEntityManagerFactoryBean nexiOracleEntityManagerFactory) {
        return new JpaTransactionManager(nexiOracleEntityManagerFactory.getObject());
    }
}