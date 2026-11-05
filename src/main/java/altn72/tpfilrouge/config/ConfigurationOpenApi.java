package altn72.tpfilrouge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigurationOpenApi {

    @Bean
    public OpenAPI documentationApi() {
        return new OpenAPI().info(new Info()
                .title("API de supervision des incidents")
                .version("1.0")
                .description("L'API utilise la session de l'application : il faut d'abord se connecter sur /login. "
                        + "Les erreurs sont renvoyées au format application/problem+json, avec un message en français "
                        + "dans le champ « detail »."));
    }
}
