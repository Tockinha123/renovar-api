package com.tocka.renovarAPI.infra.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.tocka.renovarAPI.user.User;

@Service
public class TokenService {
    
    @Value("${api.security.jwt.secret}")
    private String secret; 

    @Value("${api.security.jwt.expiration-s}")
    private long EXPIRATION_SEC;

    public String generateToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secret);

        return JWT.create()
                .withIssuer("renovarAPI")
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("roles", user.getRoles().stream().map(Enum::name).toList())
                .withExpiresAt(Instant.now().plusSeconds(EXPIRATION_SEC))
                .sign(algorithm);
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("renovarAPI")
                    .build()
                    .verify(token)
                    .getClaim("email")
                    .asString();
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}