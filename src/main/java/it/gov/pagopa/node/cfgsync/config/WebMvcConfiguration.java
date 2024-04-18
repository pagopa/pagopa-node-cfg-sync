package it.gov.pagopa.node.cfgsync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.node.cfgsync.model.AppCorsConfiguration;
import lombok.SneakyThrows;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

  @Value("${cors.configuration}")
  private String corsConfiguration;

  @Autowired
  private ViewNamingStrategy viewNamingStrategy;


  @SneakyThrows
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    AppCorsConfiguration appCorsConfiguration = new ObjectMapper().readValue(corsConfiguration,
        AppCorsConfiguration.class);
    registry.addMapping("/**")
        .allowedOrigins(appCorsConfiguration.getOrigins())
        .allowedMethods(appCorsConfiguration.getMethods());
  }

  @Bean
  public ImplicitNamingStrategy implicit() {
    return new ImplicitNamingStrategyLegacyJpaImpl();
  }

}


