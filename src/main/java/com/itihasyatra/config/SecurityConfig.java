package com.itihasyatra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF to allow POST/PUT/DELETE requests from React/Swagger
            .csrf(csrf -> csrf.disable())
            
            // 2. Configure CORS so your React app on port 5173 can talk to Spring
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 3. Define which URLs are public and which are protected
            .authorizeHttpRequests(auth -> auth
                // Public Auth Endpoints (Signup, Login, Verification)
                .requestMatchers("/api/auth/**").permitAll()
                
                // Public Trip Endpoints
                .requestMatchers("/api/trips/**").permitAll()
                
                // --- ADDED THIS LINE TO FIX THE 403 ERROR ---
                .requestMatchers("/api/guide-applications/**").permitAll()
                
                // Public Content Endpoints (Images, etc.)
                .requestMatchers("/api/content/**").permitAll()
                
                // Swagger Documentation
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // Any other request must be authenticated
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173")); 
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}