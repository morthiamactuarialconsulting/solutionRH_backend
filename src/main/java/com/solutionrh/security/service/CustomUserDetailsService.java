package com.solutionrh.security.service;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.solutionrh.dao.EmployerRepository;
import com.solutionrh.model.Employer;
import com.solutionrh.security.model.UserEntity;
import com.solutionrh.security.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final EmployerRepository employerRepository;
    private final UserRepository userRepository;

    public CustomUserDetailsService(EmployerRepository employerRepository,
                                   UserRepository userRepository) {
        this.employerRepository = employerRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Tentative d'authentification pour l'utilisateur: {}", email);
        
        // Stratégie de recherche double:
        // 1. D'abord essayer de trouver l'utilisateur dans UserEntity
        UserEntity userEntity = null;
        try {
            userEntity = userRepository.findByUsername(email)
                    .orElse(null);
        } catch (Exception e) {
            logger.warn("Erreur lors de la recherche dans UserRepository: {}", e.getMessage());
        }

        // 2. Si trouvé dans UserEntity, utiliser ces informations
        if (userEntity != null) {
            logger.debug("Utilisateur trouvé dans UserEntity: {}", userEntity.getUsername());
            Collection<GrantedAuthority> authorities = userEntity.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toList());
            
            return new org.springframework.security.core.userdetails.User(
                    userEntity.getUsername(),
                    userEntity.getPassword(),
                    true, true, true, true,
                    authorities);
        }
        
        // 3. Sinon, chercher dans Employer
        logger.debug("Recherche de l'utilisateur dans Employer: {}", email);
        Employer employer = employerRepository.findByProfessionalEmail(email)
                .orElseThrow(() -> {
                    logger.error("Utilisateur non trouvé: {}", email);
                    return new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email);
                });
        
        // Vérification de l'état du compte
        if (employer.getAccountStatus() != Employer.AccountStatus.ACTIVE) {
            logger.warn("Tentative de connexion à un compte non actif: {}, status: {}", 
                    email, employer.getAccountStatus());
            throw new UsernameNotFoundException("Compte inactif ou en attente de vérification: " + email);
        }
        
        // L'employeur implémente déjà UserDetails, donc on peut le retourner directement
        logger.debug("Utilisateur authentifié avec succès: {}", email);
        return employer;
    }
}
