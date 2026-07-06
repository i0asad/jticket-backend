package com.jticket.jticket_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public routes
                        .requestMatchers("/api/auth/**").permitAll()

                        // Client/Admin only
                        .requestMatchers(HttpMethod.POST, "/api/tickets").hasAnyRole("CLIENT", "ADMIN")

                        // Admin routes
                        .requestMatchers(HttpMethod.GET, "/api/users/search-workers").hasAnyRole("MOD", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/*/revive").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/tickets/**").hasRole("ADMIN")

                        // Mod routes
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/*/approve").hasAnyRole("MOD", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/*/reject").hasAnyRole("MOD", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/*/assign").hasAnyRole("MOD", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/*/cancel").hasAnyRole("MOD", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pass-requests/*/approve").hasAnyRole("MOD", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pass-requests/*/deny").hasAnyRole("MOD", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/pass-requests/pending").hasAnyRole("MOD", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/all").hasAnyRole("MOD", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/status/**").hasAnyRole("MOD", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/search").hasAnyRole("MOD", "ADMIN")

                        // Auth required
                        .requestMatchers("/api/**").authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200")); // Allow local dev
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
