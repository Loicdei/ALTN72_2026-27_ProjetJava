package altn72.tpfilrouge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ConfigurationHorloge {

    // L'horloge est injectée pour que les tests puissent fixer "maintenant" et vérifier les règles liées au temps.
    @Bean
    public Clock horloge() {
        return Clock.systemDefaultZone();
    }
}
