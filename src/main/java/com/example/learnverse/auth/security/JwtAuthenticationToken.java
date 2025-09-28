package com.example.learnverse.auth.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import io.jsonwebtoken.Claims;

import java.util.Collection;
import java.util.Map;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private Object credentials;
    @Getter
    private final Claims claims;

    public JwtAuthenticationToken(Object principal,
                                  Object credentials,
                                  Collection<? extends GrantedAuthority> authorities,
                                  Claims claims) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.claims = claims;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    // Make claims accessible via details for SpEL expressions
    @Override
    public Object getDetails() {
        return claims;
    }
}