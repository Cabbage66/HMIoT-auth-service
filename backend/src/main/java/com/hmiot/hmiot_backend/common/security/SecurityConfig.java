package com.hmiot.hmiot_backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmiot.hmiot_backend.common.exception.ErrorCode;
import com.hmiot.hmiot_backend.common.response.ApiResponse;
import com.hmiot.hmiot_backend.module.ticket.config.TicketProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, TicketProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/health",
                                "/api/v1/auth/**",
                                "/api/v1/registrations/**",
                                "/api/v1/tickets/verify",
                                "/api/v1/sso/validate",
                                "/api/v1/mock-business-app/access",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/dynamic-biometrics/reports", "/api/v1/iot-device-statuses/reports").permitAll()
                        .requestMatchers("/api/v1/iot-device-statuses/check", "/api/v1/terminal-events/reports", "/api/v1/terminal-events/my/recent", "/api/v1/trust-assessments/check").authenticated()
                        .requestMatchers("/api/v1/users/**", "/api/v1/admin/registrations/**", "/api/v1/certificates/**", "/api/v1/apps/**", "/api/v1/devices/**", "/api/v1/terminals/**", "/api/v1/bindings/**", "/api/v1/auth-methods/**", "/api/v1/auth-policies/**", "/api/v1/security-baselines/**", "/api/v1/policies/publish-records", "/api/v1/biometric-templates/**", "/api/v1/biometric-auth-records/**", "/api/v1/iot-device-statuses/**", "/api/v1/terminal-events/**", "/api/v1/trust-assessments/**").hasAnyRole("SYSTEM_ADMIN", "ADMIN")
                        .requestMatchers("/api/v1/voiceprints/**", "/api/v1/dynamic-biometrics/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> writeError(response, objectMapper, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeError(response, objectMapper, ErrorCode.FORBIDDEN))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void writeError(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode));
    }
}
