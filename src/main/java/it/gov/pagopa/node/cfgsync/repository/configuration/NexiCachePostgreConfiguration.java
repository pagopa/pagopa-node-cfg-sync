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
@PropertySources({
        @PropertySource("classpath:/application.properties"),
        @PropertySource(value = "classpath:/application-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
})
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "it.gov.pagopa.node.cfgsync.repository.nexipostgre.cache",
        entityManagerFactoryRef = "nexiCachePostgreEntityManagerFactory",
        transactionManagerRef = "nexiCachePostgreTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.nexi.postgre.cache", name = "enabled")
public class NexiCachePostgreConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.postgre.cache")
    public DataSourceProperties nexiCachePostgreDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.postgre.cache.configuration")
    public DataSource nexiCachePostgreDataSource() {
        return nexiCachePostgreDatasourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = "nexiCachePostgreEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean nexiCachePostgreEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(nexiCachePostgreDataSource())
                .packages(ConfigCache.class)
                .build();
    }

    @Bean
    public PlatformTransactionManager nexiCachePostgreTransactionManager(
            final @Qualifier("nexiCachePostgreEntityManagerFactory") LocalContainerEntityManagerFactoryBean nexiCachePostgreEntityManagerFactory) {
        return new JpaTransactionManager(nexiCachePostgreEntityManagerFactory.getObject());
    }

}
