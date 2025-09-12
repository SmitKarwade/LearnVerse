package com.example.learnverse.auth.config;

import com.example.learnverse.auth.security.JwtAuthFilter;
import com.example.learnverse.auth.security.RestAccessDeniedHandler;
import com.example.learnverse.auth.security.RestAuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthEntryPoint authEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Disable CSRF and set stateless session
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Exception handling
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        // Authorization rules - FIXED WILDCARDS
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/actuator/health", "/api/hello").permitAll()         // Public endpoints
                .requestMatchers("/api/tutor/**").hasRole("TUTOR")                                 // Tutor-only endpoints
                .requestMatchers("/api/user/**").hasRole("USER")                                   // User-only endpoints (including interests)
                .requestMatchers("/api/activities/create").hasRole("TUTOR")                         // Only tutors create activity
                .requestMatchers("/api/activities/filter").hasRole("USER")
                .requestMatchers("/api/activities/filter/**").hasRole("USER")
                .requestMatchers("/api/**").hasAnyRole("USER", "TUTOR")                            // Both can access other /api endpoints
                .anyRequest().authenticated()
        );

        // Add JWT filter
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}