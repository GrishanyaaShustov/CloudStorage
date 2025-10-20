package cloud.storage.authservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

    private final JwtValidator jwtValidator;

    public SecurityConfiguration(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {

        http
                // 1. Отключаем сессию, используем stateless (JWT проверяется на каждом запросе)
                .sessionManagement(Manage -> Manage.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 2. Настройка правил авторизации
                .authorizeHttpRequests(Authorize -> Authorize
                        .requestMatchers("/api/**").authenticated() // все запросы к /api/** требуют авторизации
                        .anyRequest().permitAll()) // остальные доступны всем

                // 4. Добавляем JwtValidator перед стандартным BasicAuthenticationFilter
                .addFilterBefore(jwtValidator, BasicAuthenticationFilter.class)

                // 5. Отключаем CSRF, так как JWT защищает от CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 6. Настройка CORS через кастомный источник конфигурации
                .cors(cors -> cors.configurationSource(corsConfigurationSource));

        return http.build(); // возвращаем готовую цепочку фильтров
    }


    // Метод создает конфигурацию CORS для разрешения запросов с фронтенда
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration cfg = new CorsConfiguration();

            // Разрешенные домены фронтенда
            cfg.setAllowedOrigins(Arrays.asList(
                    "http://localhost:3000",
                    "http://localhost:5173"
            ));

            // Разрешенные HTTP-методы
            cfg.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS", "PATCH"));

            // Разрешенные заголовки
            cfg.setAllowedHeaders(List.of("*"));

            // Разрешаем отправку авторизационных заголовков и куки
            cfg.setAllowCredentials(true);

            // Заголовки, которые клиент может прочитать
            cfg.setExposedHeaders(List.of("Authorization"));

            // Время жизни preflight запроса в секундах
            cfg.setMaxAge(3600L);

            return cfg;
        };
    }

    // BCryptPasswordEncoder для кодирования и проверки паролей
    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    // Bean для AuthenticationManager, нужен для ручной аутентификации (логин + пароль)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
