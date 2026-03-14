package com.aquagrow.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.http.HttpMethod.OPTIONS;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/telegram/webhook").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("Admin")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter() {
        org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter();

        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter converter = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = defaultAuthoritiesConverter
                    .convert(jwt);

            // 1. Extract realm_access.roles
            java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                authorities.addAll(roles.stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + role))
                        .toList());
            }

            // 2. Extract resource_access (Client Roles)
            java.util.Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                resourceAccess.forEach((client, clientObject) -> {
                    if (clientObject instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> clientMap = (java.util.Map<String, Object>) clientObject;
                        if (clientMap.containsKey("roles")) {
                            @SuppressWarnings("unchecked")
                            java.util.List<String> roles = (java.util.List<String>) clientMap.get("roles");
                            authorities.addAll(roles.stream()
                                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                            "ROLE_" + role))
                                    .toList());
                        }
                    }
                });
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse comma-separated origins and trim whitespace
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .peek(origin -> log.info("Adding Allowed Origin Pattern: {}", origin))
                .toList();

        // Use allowedOriginPatterns instead of allowedOrigins to support "*" with credentials
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
