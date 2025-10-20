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
                // PUBLIC endpoints
                .requestMatchers("/auth/**", "/actuator/health", "/api/hello", "/api/test/**").permitAll()

                // ⭐ ADMIN ENDPOINTS - PUT THESE FIRST (MOST SPECIFIC)
                .requestMatchers("/api/tutor-verification/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/files/**").hasRole("ADMIN")

                // USER-only endpoints (tutor verification registration)
                .requestMatchers(HttpMethod.POST, "/api/tutor-verification/register").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/tutor-verification/status/**").hasRole("USER")

                // WebSocket
                .requestMatchers("/ws/community").hasAnyRole("USER", "TUTOR", "ADMIN")

                // Community follow system
                .requestMatchers(HttpMethod.POST, "/api/community/follow/*").hasAnyRole("USER", "TUTOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/community/follow/*").hasAnyRole("USER", "TUTOR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/community/follow/**").hasAnyRole("USER", "TUTOR", "ADMIN")

                // Community posts - TUTOR creates, everyone views
                .requestMatchers(HttpMethod.POST, "/api/community/posts").hasAnyRole("TUTOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/community/posts/*").hasAnyRole("TUTOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/community/posts/*").hasAnyRole("TUTOR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/community/posts/**").hasAnyRole("USER", "TUTOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/community/posts/*/like").hasAnyRole("USER", "TUTOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/community/posts/*/share").hasAnyRole("USER", "TUTOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/community/posts/*/comments").hasAnyRole("USER", "TUTOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/community/posts/*/comments/*/like").hasAnyRole("USER", "TUTOR", "ADMIN")

                // TUTOR-only endpoints
                .requestMatchers(HttpMethod.POST, "/api/activities/create").hasRole("TUTOR")
                .requestMatchers(HttpMethod.PUT, "/api/activities/**").hasRole("TUTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/activities/**").hasRole("TUTOR")
                .requestMatchers("/api/activities/my-activities").hasRole("TUTOR")
                .requestMatchers("/api/tutor/**").hasRole("TUTOR")

                // Activities browsing - USER and TUTOR
                .requestMatchers(HttpMethod.GET, "/api/activities/**").hasAnyRole("USER", "TUTOR")
                .requestMatchers("/api/activities/filter").hasAnyRole("USER", "TUTOR")
                .requestMatchers("/api/activities/filter/**").hasAnyRole("USER", "TUTOR")
                .requestMatchers("/api/activities/search/**").hasAnyRole("USER", "TUTOR")
                .requestMatchers("/api/activities/all").hasAnyRole("USER", "TUTOR")
                .requestMatchers("/api/activities/my-feed").hasAnyRole("USER", "TUTOR")

                // Enrollments
                .requestMatchers("/api/enrollments/**").hasAnyRole("USER", "TUTOR", "ADMIN")

                // Debug
                .requestMatchers("/api/debug/**").hasAnyRole("USER", "TUTOR", "ADMIN")

                // User endpoints
                .requestMatchers("/api/user/**").hasAnyRole("USER", "TUTOR")

                // Fallback
                .requestMatchers("/api/**").hasAnyRole("USER", "TUTOR")
                .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
