package fr.edc3.pmt.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pmtOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PMT API")
                        .version("v1")
                    .description("API de gestion de projets, de workspaces, de tâches et de notifications.")
                        .contact(new Contact().name("Equipe PMT")))
                .addServersItem(new Server().url("/").description("Serveur courant"));
    }
}
