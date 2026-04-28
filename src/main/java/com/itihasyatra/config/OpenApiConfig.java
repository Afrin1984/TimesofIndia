package com.itihasyatra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI itihasYatraOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("ItihasYatra API")
                .description("Backend API documentation for India's Rich Heritage platform")
                .version("v1.0"));
    }
}