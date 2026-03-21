package com.glycowatch.backend.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String getIssuer() {
        return jwtProperties.issuer();
    }

    public boolean isTokenValid(String token) {
        // Placeholder until auth module is implemented.
        return token != null && !token.isBlank();
    }
}

