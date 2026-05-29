package com.hmiot.hmiot_backend.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.isValid(token)) {
            Claims claims = jwtTokenProvider.parseClaims(token);
            String principalType = claims.get("principalType", String.class);
            if (principalType == null || "ADMIN".equals(principalType)) {
                CurrentAdmin currentAdmin = new CurrentAdmin(
                        Long.valueOf(claims.getSubject()),
                        claims.get("username", String.class),
                        claims.get("displayName", String.class),
                        claims.get("roleCode", String.class)
                );
                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(currentAdmin));
            } else if ("USER".equals(principalType)) {
                CurrentUser currentUser = new CurrentUser(
                        Long.valueOf(claims.getSubject()),
                        claims.get("username", String.class),
                        claims.get("realName", String.class),
                        claims.get("authMethod", String.class),
                        claims.get("terminalId", Long.class),
                        claims.get("terminalCode", String.class),
                        claims.get("clientType", String.class)
                );
                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(currentUser));
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
