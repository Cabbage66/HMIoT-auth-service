package com.hmiot.hmiot_backend.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal;

    public JwtAuthenticationToken(CurrentAdmin principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + principal.roleCode())));
        this.principal = principal;
        setAuthenticated(true);
    }

    public JwtAuthenticationToken(CurrentUser principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
