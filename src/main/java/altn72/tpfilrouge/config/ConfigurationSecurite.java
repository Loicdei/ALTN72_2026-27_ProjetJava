package altn72.tpfilrouge.config;

import altn72.tpfilrouge.modele.UtilisateurDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConfigurationSecurite {

    @Bean
    public PasswordEncoder hacheurDeMotDePasse() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtreDeSecurite(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/incidents/*/affectation")
                        .hasRole(UtilisateurDetails.PROFIL_RESPONSABLE_EXPLOITATION)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/accueil", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }
}