package com.example.learnverse.auth.config;

import com.example.learnverse.auth.security.JwtAuthFilter;
import com.example.learnverse.auth.security.RestAccessDeniedHandler;
import com.example.learnverse.auth.security.RestAuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        http.authorizeHttpRequests(auth -> auth
                // PUBLIC - Anyone can register as basic user
                .requestMatchers("/auth/**", "/actuator/health", "/api/hello").permitAll()

                // AUTHENTICATED USER - Can request tutor verification
                .requestMatchers(HttpMethod.POST, "/api/tutor-verification/register").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/tutor-verification/status/**").hasRole("USER")

                // TEMPORARILY PUBLIC for testing - REMOVE THIS LATER!
                .requestMatchers("/api/tutor-verification/admin/**").permitAll()

                // ADMIN ONLY - Review and approve verifications
                .requestMatchers("/api/tutor-verification/admin/**").hasRole("ADMIN")

                // APPROVED TUTOR ONLY - Must have TUTOR role AND approved status
                .requestMatchers("/api/activities/create").hasRole("TUTOR")
                .requestMatchers("/api/tutor/**").hasRole("TUTOR")

                // REGULAR USER - Can browse activities
                .requestMatchers("/api/user/**").hasRole("USER")
                .requestMatchers("/api/activities/filter").hasRole("USER")
                .requestMatchers("/api/activities/filter/**").hasRole("USER")

                // Both roles can access general API
                .requestMatchers("/api/**").hasAnyRole("USER", "TUTOR")
                .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}