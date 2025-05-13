package com.solutionrh.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.solutionrh.security.jwt.JwtAuthenticationFilter;
import com.solutionrh.security.service.CustomUserDetailsService;
import com.solutionrh.security.jwt.JwtGenerator;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtGenerator jwtGenerator, CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtGenerator, userDetailsService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        authz -> authz
                                // Autoriser toutes les requêtes OPTIONS sans authentification (crucial pour CORS)
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                
                                // Routes d'authentification publiques
                                .requestMatchers("/api/auth/login").permitAll()
                                .requestMatchers("/api/auth/register").permitAll()
                                .requestMatchers("/api/auth/refresh").permitAll()
                                .requestMatchers("/api/auth/forgot-password").permitAll()
                                .requestMatchers("/api/auth/reset-password").permitAll()
                                .requestMatchers("/api/auth/register-with-files").permitAll()
                                
                                // Routes protégées
                                .requestMatchers("/api/employers/**").hasAnyAuthority("ADMIN")
                                .requestMatchers("/api/candidates/**").hasAnyAuthority("ADMIN", "EMPLOYER")
                                .requestMatchers("/api/jobs/**").permitAll()
                                
                                // Routes d'administration (nécessitent le rôle ADMIN)
                                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                                
                                // Toutes les autres requêtes nécessitent une authentification
                                .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
