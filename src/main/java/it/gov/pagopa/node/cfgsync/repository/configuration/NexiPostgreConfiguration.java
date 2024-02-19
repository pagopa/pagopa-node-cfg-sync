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
        basePackages = "it.gov.pagopa.node.cfgsync.repository.nexipostgres",
        entityManagerFactoryRef = "nexiCachePostgreEntityManagerFactory",
        transactionManagerRef = "nexiCachePostgreTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.nexi.postgres", name = "enabled")
public class NexiPostgreConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.postgres")
    public DataSourceProperties nexiPostgreDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.nexi.postgres")
    public DataSource nexiPostgreDataSource() {
        return nexiPostgreDatasourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Bean(name = "nexiPostgreEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean nexiPostgreEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(nexiPostgreDataSource())
                .packages(ConfigCache.class)
                .build();
    }

    @Bean
    public PlatformTransactionManager nexiPostgreTransactionManager(
            final @Qualifier("nexiPostgreEntityManagerFactory") LocalContainerEntityManagerFactoryBean nexiPostgreEntityManagerFactory) {
        return new JpaTransactionManager(nexiPostgreEntityManagerFactory.getObject());
    }

}
