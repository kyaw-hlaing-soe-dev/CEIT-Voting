package com.KTU.KTUVotingapp.config;

import com.KTU.KTUVotingapp.model.UserRole;
import com.KTU.KTUVotingapp.model.Voter;
import com.KTU.KTUVotingapp.repository.VoterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;

/**
 * Security configuration for Role-Based Access Control (RBAC).
 * 
 * Implements physical separation between Admin and User endpoints:
 * - Admin endpoints (/api/admin/**) require ROLE_ADMIN authority
 * - User voting endpoints (/api/voting/**) require ROLE_USER or ROLE_ADMIN authority
 * - Public endpoints (auth, static resources) are accessible to everyone
 * 
 * Admin votes count as 2, User votes count as 1 (weighted voting).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${voting.admin-pin:}")
    private String adminPin;

    @Value("${voting.user-pin:}")
    private String userPin;

    /**
     * Security filter chain for Admin endpoints.
     * Admin endpoints are physically separated and protected.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/admin/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .anyRequest().denyAll()
            );
        return http.build();
    }

    /**
     * Security filter chain for voting endpoints.
     * Both users and admins can vote, but with different weights.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain votingSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/voting/**", "/api/votes/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/voting/**", "/api/votes/**")
                    .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                .anyRequest().denyAll()
            );
        return http.build();
    }

    /**
     * Default security filter chain for public endpoints.
     * Allows access to authentication endpoints, static resources, and actuator.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints
                .requestMatchers("/api/auth/**").permitAll()
                // Public endpoints for viewing candidates and results
                .requestMatchers(HttpMethod.GET, "/api/candidates/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/results/**").permitAll()
                // Static resources
                .requestMatchers("/", "/index.html", "/vote.html", "/admin.html", 
                    "/styles.css", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Actuator endpoints for health checks
                .requestMatchers("/actuator/**").permitAll()
                // Any other request requires authentication
                .anyRequest().permitAll()
            );
        return http.build();
    }

    /**
     * Custom authentication manager that validates PINs and assigns appropriate roles.
     * This is used by the AuthController to authenticate users.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return authentication -> {
            String pin = authentication.getPrincipal().toString();
            
            if (adminPin != null && !adminPin.isEmpty() && adminPin.equals(pin)) {
                return new UsernamePasswordAuthenticationToken(
                    pin, 
                    null, 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
            }
            
            if (userPin != null && !userPin.isEmpty() && userPin.equals(pin)) {
                return new UsernamePasswordAuthenticationToken(
                    pin, 
                    null, 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
            }
            
            throw new BadCredentialsException("Invalid PIN");
        };
    }

    /**
     * Determines the UserRole based on the PIN.
     * Admin PIN gets ROLE_ADMIN, User PIN gets ROLE_USER.
     */
    public UserRole getRoleByPin(String pin) {
        if (adminPin != null && !adminPin.isEmpty() && adminPin.equals(pin)) {
            return UserRole.ROLE_ADMIN;
        }
        return UserRole.ROLE_USER;
    }
}
