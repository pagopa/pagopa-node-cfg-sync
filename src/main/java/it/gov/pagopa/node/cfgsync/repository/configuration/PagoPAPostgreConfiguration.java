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
        basePackages = "it.gov.pagopa.node.cfgsync.repository.pagopa",
        entityManagerFactoryRef = "pagoPACachePostgreEntityManagerFactory",
        transactionManagerRef = "pagoPACachePostgreTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.pagopa.postgre", name = "enabled")
public class PagoPAPostgreConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.pagopa.postgre")
    public DataSourceProperties pagoPACachePostgreDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.pagopa.postgre.configuration")
    public DataSource pagoPACachePostgreDataSource() {
        return pagoPACachePostgreDatasourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    @Primary
    @Bean(name = "pagoPACachePostgreEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean pagoPACachePostgreEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(pagoPACachePostgreDataSource())
                .packages(ConfigCache.class)
                .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager pagoPACachePostgreTransactionManager(
            final @Qualifier("pagoPACachePostgreEntityManagerFactory") LocalContainerEntityManagerFactoryBean pagoPACachePostgreEntityManagerFactory) {
        return new JpaTransactionManager(pagoPACachePostgreEntityManagerFactory.getObject());
    }

}
