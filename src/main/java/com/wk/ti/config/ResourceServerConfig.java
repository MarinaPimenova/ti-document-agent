package com.wk.ti.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("RedundantThrows")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

    private final InvalidTokenAuthenticationEntryPoint invalidTokenAuthenticationEntryPoint;
    private final ForbiddenHandler forbiddenHandler;
    private final CustomGrantedAuthoritiesConverter customGrantedAuthoritiesConverter;
    private final String allowedOrigins;

    public ResourceServerConfig(
            InvalidTokenAuthenticationEntryPoint invalidTokenAuthenticationEntryPoint,
            ForbiddenHandler forbiddenHandler,
            CustomGrantedAuthoritiesConverter customGrantedAuthoritiesConverter,
            @Value("${ms.cors.allowed-origins}")
            String allowedOrigins) {
        this.invalidTokenAuthenticationEntryPoint = invalidTokenAuthenticationEntryPoint;
        this.forbiddenHandler = forbiddenHandler;
        this.customGrantedAuthoritiesConverter = customGrantedAuthoritiesConverter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.exceptionHandling(exceptionHandling ->
                exceptionHandling
                        .authenticationEntryPoint(invalidTokenAuthenticationEntryPoint)
                        .accessDeniedHandler(forbiddenHandler));

        http.authorizeHttpRequests(
                request ->
                        request.requestMatchers(
                                        "/v3/**",
                                        "/swagger-ui/**",
                                        "/version",
                                        "/oauth2/**",
                                        "/rest/**",
                                        "/actuator/**",
                                        "/index.html",
                                        "/static/**",
                                        "/fonts/**",
                                        "/styles/**",
                                        "/icons/**",
                                        "/error/**",
                                        "/*.ico",
                                        "/*.json",
                                        "/*.png")
                                .permitAll()
                                .requestMatchers("/api/**", "/")
                                .authenticated()
        ).oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )).cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(customGrantedAuthoritiesConverter);
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}

