package com.solutionrh.security.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.solutionrh.security.model.PasswordResetToken;
import com.solutionrh.security.model.UserEntity;
import com.solutionrh.security.repository.PasswordResetTokenRepository;
import com.solutionrh.security.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordService.class);
    
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Vérifie si le mot de passe actuel est correct pour un utilisateur donné
     */
    public boolean isCurrentPasswordValid(String username, String currentPassword) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            return passwordEncoder.matches(currentPassword, userOpt.get().getPassword());
        }
        return false;
    }
    
    /**
     * Change le mot de passe d'un utilisateur
     */
    @Transactional
    public void changePassword(String username, String newPassword) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            logger.info("Mot de passe modifié avec succès pour l'utilisateur: {}", username);
        } else {
            logger.error("Utilisateur non trouvé lors de la modification du mot de passe: {}", username);
            throw new RuntimeException("Utilisateur non trouvé: " + username);
        }
    }
    
    /**
     * Crée un token de réinitialisation pour un utilisateur
     */
    @Transactional
    public String createPasswordResetTokenForUser(String username) {
        // Vérifier si l'utilisateur existe
        if (!userRepository.existsByUsername(username)) {
            logger.error("Tentative de création d'un token de réinitialisation pour un utilisateur inexistant: {}", username);
            throw new RuntimeException("Utilisateur non trouvé: " + username);
        }
        
        // Supprimer les tokens existants pour cet utilisateur
        tokenRepository.findByUsername(username).ifPresent(tokenRepository::delete);
        
        // Créer un nouveau token
        PasswordResetToken token = new PasswordResetToken(username);
        tokenRepository.save(token);
        
        logger.info("Token de réinitialisation créé pour l'utilisateur: {}", username);
        return token.getToken();
    }
    
    /**
     * Valide un token de réinitialisation
     */
    public boolean validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            logger.warn("Token de réinitialisation non trouvé: {}", token);
            return false;
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isExpired()) {
            logger.warn("Token de réinitialisation expiré: {}", token);
            tokenRepository.delete(resetToken);
            return false;
        }
        
        return true;
    }
    
    /**
     * Réinitialise le mot de passe avec un token
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            logger.error("Token de réinitialisation non trouvé lors de la réinitialisation: {}", token);
            throw new RuntimeException("Token de réinitialisation invalide");
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isExpired()) {
            logger.error("Tentative d'utilisation d'un token expiré: {}", token);
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Token de réinitialisation expiré");
        }
        
        String username = resetToken.getUsername();
        changePassword(username, newPassword);
        
        // Supprimer le token après utilisation
        tokenRepository.delete(resetToken);
        logger.info("Mot de passe réinitialisé avec succès pour l'utilisateur: {}", username);
    }
    
    /**
     * Nettoie les tokens expirés (exécuté tous les jours à minuit)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        logger.info("Nettoyage des tokens de réinitialisation expirés effectué");
    }
}
