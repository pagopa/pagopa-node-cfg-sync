package it.gov.pagopa.node.cfgsync.repository.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@PropertySources({
        @PropertySource("classpath:/application.properties"),
        @PropertySource(value = "classpath:/application-${spring.profiles.active}.properties", ignoreResourceNotFound = true)
})
@EnableJpaRepositories(
        basePackages = "it.gov.pagopa.node.cfgsync.repository.nexi",
        entityManagerFactoryRef = "nodoNexiOEntityManager",
        transactionManagerRef = "nodoNexiOTransactionManager"
)
public class NodoONexiConfiguration {
    @Autowired
    private Environment env;

    @Bean
    public LocalContainerEntityManagerFactoryBean nodoNexiOEntityManager() {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(nodoNexiODataSource());
        em.setPackagesToScan("it.gov.pagopa.node.cfgsync.repository.model.nexi");

        HibernateJpaVendorAdapter vendorAdapter
                = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto",
                env.getProperty("hibernate.hbm2ddl.auto"));
        properties.put("hibernate.dialect",
                env.getProperty("hibernate.dialect"));
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    public DataSource nodoNexiODataSource() {
        DriverManagerDataSource dataSource
                = new DriverManagerDataSource();
        dataSource.setDriverClassName(
                env.getProperty("db.nodo.nexi.oracle.datasource.driverClassName"));
        dataSource.setUrl(env.getProperty("db.nodo.nexi.oracle.datasource.url"));
        dataSource.setUsername(env.getProperty("db.nodo.nexi.oracle.datasource.username"));
        dataSource.setPassword(env.getProperty("db.nodo.nexi.oracle.datasource.password"));

        return dataSource;
    }

    @Bean
    public PlatformTransactionManager nodoNexiOTransactionManager() {
        JpaTransactionManager transactionManager
                = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(
                nodoNexiOEntityManager().getObject());

        return transactionManager;
    }
}