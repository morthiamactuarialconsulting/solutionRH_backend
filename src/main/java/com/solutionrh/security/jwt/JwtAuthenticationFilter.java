package com.solutionrh.security.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;


import com.solutionrh.security.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtGenerator jwtGenerator;
    private final CustomUserDetailsService customUserDetailsService;
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = getJWTFromRequest(request);
            
            // Si pas de token ou token vide, on continue la chaîne de filtres
            if (!StringUtils.hasText(token)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Vérification si le token est expiré
            if (jwtGenerator.isTokenExpired(token)) {
                logger.warn("Token expiré détecté pour la requête: {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token expiré. Veuillez vous reconnecter ou rafraîchir votre token.");
                return;
            }
            
            try {
                // Validation du token et authentification
                if (jwtGenerator.validateToken(token)) {
                    String username = jwtGenerator.getUsernameFromJWT(token);
                    
                    try {
                        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                        
                        // Création du token d'authentification Spring Security
                        UsernamePasswordAuthenticationToken authenticationToken = 
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // Définition du contexte de sécurité
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        
                        logger.debug("Utilisateur authentifié: {} pour l'URI: {}", username, request.getRequestURI());
                    } catch (UsernameNotFoundException e) {
                        logger.error("Utilisateur {} non trouvé dans la base de données", username);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Utilisateur non trouvé");
                        return;
                    }
                }
            } catch (ExpiredJwtException e) {
                logger.warn("Token JWT expiré: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token expiré");
                return;
            } catch (SignatureException e) {
                logger.error("Signature JWT invalide: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Signature de token invalide");
                return;
            } catch (MalformedJwtException e) {
                logger.error("Token JWT malformé: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token malformé");
                return;
            } catch (Exception e) {
                logger.error("Erreur lors de la validation du token: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Erreur d'authentification");
                return;
            }
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Erreur non gérée dans le filtre JWT: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Erreur serveur lors de l'authentification");
        }
    }
    
    /**
     * Extrait le token JWT de l'en-tête Authorization de la requête HTTP
     * Format attendu: "Bearer [token]"
     * 
     * @param request La requête HTTP
     * @return Le token JWT extrait ou null si non présent/invalide
     */
    private String getJWTFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX_LENGTH);
        }
        
        // Vérification alternative dans les paramètres de requête (utile pour WebSocket)
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        
        return null;
    }
}
