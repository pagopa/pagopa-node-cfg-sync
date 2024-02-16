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
        basePackages = "it.gov.pagopa.node.cfgsync.repository.nexioracle.cache",
        entityManagerFactoryRef = "nexiCacheOracleEntityManagerFactory",
        transactionManagerRef = "nexiCacheOracleTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.nexi.oracle.cache", name = "enabled")
public class NexiCacheOracleConfiguration {

    @Autowired
    private Environment env;

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.oracle.cache")
    public DataSourceProperties nexiCacheOracleDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.oracle.cache.configuration")
    public DataSource nexiCacheOracleDataSource() {
        return nexiCacheOracleDatasourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = "nexiCacheOracleEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean nexiCacheOracleEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(nexiCacheOracleDataSource())
                .packages(ConfigCache.class)
                .build();
    }

    @Bean
    public PlatformTransactionManager nexiCacheOracleTransactionManager(
            final @Qualifier("nexiCacheOracleEntityManagerFactory") LocalContainerEntityManagerFactoryBean nexiCacheOracleEntityManagerFactory) {
        return new JpaTransactionManager(nexiCacheOracleEntityManagerFactory.getObject());
    }
}