package com.solutionrh.security.jwt;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

import jakarta.annotation.PostConstruct;

@Component
public class JwtGenerator {
    @Value("${jwt.secret:defaultSecretKey12345678901234567890123456789012}")
    private String secretKey;
    
    @Value("${jwt.expiration:3600000}")
    private long jwtExpiration; // 1 heure par défaut
    
    @Value("${jwt.refresh-expiration:86400000}")
    private long refreshExpiration; // 24 heures par défaut
    
    private Key key;

    @PostConstruct
    public void init() {
        try {
            // Utiliser la clé configurée si elle est suffisamment longue
            if (secretKey.getBytes().length >= 64) { // Au moins 512 bits pour HS512
                this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
            } else {
                // Sinon, générer une clé sécurisée pour HS512
                this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
                System.out.println("ATTENTION: La clé JWT configurée n'est pas assez longue pour HS512. "
                        + "Une clé temporaire a été générée pour cette session. "
                        + "Veuillez configurer une clé d'au moins 64 octets via la variable d'environnement JWT_SECRET.");
            }
        } catch (Exception e) {
            // En cas d'erreur, générer une clé sécurisée
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            System.out.println("ATTENTION: Erreur lors de l'initialisation de la clé JWT. "
                    + "Une clé temporaire a été générée pour cette session: " + e.getMessage());
        }
    }
    
    // Génération du token JWT
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpiration);
        
        // Ajout des rôles de l'utilisateur dans les claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(currentDate)
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // Extraction du username depuis le token
    public String getUsernameFromJWT(String token) {
        return getClaimsFromToken(token).getSubject();
    }
    
    // Extraction des rôles depuis le token
    @SuppressWarnings("unchecked")
    public java.util.List<String> getRolesFromJWT(String token) {
        Claims claims = getClaimsFromToken(token);
        return (java.util.List<String>) claims.get("roles");
    }
    
    // Méthode utilitaire pour extraire les claims du token
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Validation du token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            throw new AuthenticationCredentialsNotFoundException("Signature JWT invalide", ex);
        } catch (MalformedJwtException ex) {
            throw new AuthenticationCredentialsNotFoundException("Token JWT malformé", ex);
        } catch (ExpiredJwtException ex) {
            throw new AuthenticationCredentialsNotFoundException("Token JWT expiré", ex);
        } catch (UnsupportedJwtException ex) {
            throw new AuthenticationCredentialsNotFoundException("Token JWT non supporté", ex);
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationCredentialsNotFoundException("La chaîne de revendications JWT est vide", ex);
        }
    }
    
    // Génération d'un token de rafraîchissement
    public String generateRefreshToken(Authentication authentication) {
        String username = authentication.getName();
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + refreshExpiration);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(currentDate)
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    // Vérifier si un token est expiré
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }
}
