package com.solutionrh.security.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.solutionrh.dao.EmployerRepository;
import com.solutionrh.model.Employer;
import com.solutionrh.security.dto.LoginRequestDTO;
import com.solutionrh.security.dto.MultipartRegisterRequestDTO;
import com.solutionrh.security.dto.PasswordChangeDto;
import com.solutionrh.security.dto.PasswordResetDto;
import com.solutionrh.security.dto.PasswordResetRequestDto;
import com.solutionrh.security.dto.RefreshTokenDto;
import com.solutionrh.security.jwt.JwtGenerator;
import com.solutionrh.security.model.Role;
import com.solutionrh.security.model.UserEntity;
import com.solutionrh.security.repository.RoleRepository;
import com.solutionrh.security.repository.UserRepository;
import com.solutionrh.security.service.CustomUserDetailsService;
import com.solutionrh.security.service.PasswordService;
import com.solutionrh.service.FileStorageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final JwtGenerator jwtGenerator;
    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordService passwordService;
    private final FileStorageService fileStorageService;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    /**
     * Endpoint pour modifier le mot de passe d'un utilisateur connecté
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody PasswordChangeDto passwordChangeDto,
            Authentication authentication) {
        
        try {
            // 1. Vérifier que l'utilisateur est authentifié
            if (authentication == null || !authentication.isAuthenticated()) {
                return new ResponseEntity<>(Map.of("message", "Utilisateur non authentifié"), 
                        HttpStatus.UNAUTHORIZED);
            }
            
            // 2. Récupérer le nom d'utilisateur (email) depuis l'authentification
            String username = authentication.getName();
            
            // 3. Vérifier que les mots de passe correspondent
            if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getConfirmPassword())) {
                return new ResponseEntity<>(Map.of("message", "Le nouveau mot de passe et sa confirmation ne correspondent pas"), 
                        HttpStatus.BAD_REQUEST);
            }
            
            // 4. Vérifier que le mot de passe actuel est correct
            if (!passwordService.isCurrentPasswordValid(username, passwordChangeDto.getCurrentPassword())) {
                return new ResponseEntity<>(Map.of("message", "Le mot de passe actuel est incorrect"), 
                        HttpStatus.BAD_REQUEST);
            }
            
            // 5. Mettre à jour le mot de passe
            passwordService.changePassword(username, passwordChangeDto.getNewPassword());
            
            // 6. Envoyer une notification par email
            //emailService.sendPasswordChangeNotification(username);
            
            logger.info("Mot de passe modifié avec succès pour l'utilisateur: {}", username);
            
            return new ResponseEntity<>(Map.of("message", "Mot de passe modifié avec succès"), HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la modification du mot de passe: {}", e.getMessage());
            return new ResponseEntity<>(Map.of("message", "Erreur lors de la modification du mot de passe: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Endpoint pour demander une réinitialisation de mot de passe
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto requestDto) {
        try {
            String email = requestDto.getEmail();
            
            // 1. Vérifier si l'utilisateur existe
            if (!userRepository.existsByUsername(email)) {
                // Pour des raisons de sécurité, ne pas révéler que l'utilisateur n'existe pas
                return new ResponseEntity<>(Map.of("message", "Si votre email est enregistré, vous recevrez un lien de réinitialisation"), 
                        HttpStatus.OK);
            }
            
            // 2. Générer un token de réinitialisation
            //String token = passwordService.createPasswordResetTokenForUser(email);
            
            // 3. Envoyer l'email avec le lien de réinitialisation
            //emailService.sendPasswordResetEmail(email, token);
            
            logger.info("Demande de réinitialisation de mot de passe pour: {}", email);
            
            return new ResponseEntity<>(Map.of("message", "Si votre email est enregistré, vous recevrez un lien de réinitialisation"), 
                    HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la demande de réinitialisation: {}", e.getMessage());
            return new ResponseEntity<>(Map.of("message", "Erreur lors de la demande de réinitialisation"),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Endpoint pour réinitialiser le mot de passe avec un token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetDto resetDto) {
        try {
            // 1. Vérifier que les mots de passe correspondent
            if (!resetDto.getNewPassword().equals(resetDto.getConfirmPassword())) {
                return new ResponseEntity<>(Map.of("message", "Le nouveau mot de passe et sa confirmation ne correspondent pas"), 
                        HttpStatus.BAD_REQUEST);
            }
            
            // 2. Valider le token
            if (!passwordService.validatePasswordResetToken(resetDto.getToken())) {
                return new ResponseEntity<>(Map.of("message", "Le token de réinitialisation est invalide ou expiré"), 
                        HttpStatus.BAD_REQUEST);
            }
            
            // 3. Réinitialiser le mot de passe
            passwordService.resetPassword(resetDto.getToken(), resetDto.getNewPassword());
            
            logger.info("Mot de passe réinitialisé avec succès via token");
            
            return new ResponseEntity<>(Map.of("message", "Mot de passe réinitialisé avec succès"), HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la réinitialisation du mot de passe: {}", e.getMessage());
            return new ResponseEntity<>(Map.of("message", "Erreur lors de la réinitialisation du mot de passe: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginDto) {
        try {
            // Validation explicite des champs
            if (loginDto.getUsername() == null || loginDto.getUsername().trim().isEmpty()) {
                logger.warn("Tentative de connexion avec un nom d'utilisateur vide");
                return new ResponseEntity<>(Map.of("message", "Le nom d'utilisateur est obligatoire"), 
                        HttpStatus.BAD_REQUEST);
            }
            
            if (loginDto.getPassword() == null || loginDto.getPassword().trim().isEmpty()) {
                logger.warn("Tentative de connexion avec un mot de passe vide pour l'utilisateur: {}", loginDto.getUsername());
                return new ResponseEntity<>(Map.of("message", "Le mot de passe est obligatoire"), 
                        HttpStatus.BAD_REQUEST);
            }
            
            logger.info("Tentative de connexion pour l'utilisateur: {}", loginDto.getUsername());
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));

            String token = jwtGenerator.generateToken(authentication);
            String refreshToken = jwtGenerator.generateRefreshToken(authentication);
            
            // Récupérer les rôles de l'utilisateur
            java.util.List<String> roles = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .collect(java.util.stream.Collectors.toList());
            
            logger.info("Rôles de l'utilisateur {}: {}", loginDto.getUsername(), roles);

            // Créer une réponse qui correspond exactement à la structure AuthResponse attendue par le frontend
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", token);  // Renommé 'token' à 'accessToken'
            response.put("refreshToken", refreshToken);
            
            response.put("username", loginDto.getUsername());
            response.put("roles", roles);
            
            // Récupérer toutes les informations de lemployeur à partir de l'email
            userRepository.findByUsername(loginDto.getUsername()).ifPresent(user -> {
             
                employerRepository.findByProfessionalEmail(user.getUsername()).ifPresent(employer -> {
                    response.put("id", employer.getId());
                    response.put("companyName", employer.getCompanyName());
                    response.put("ninea", employer.getNinea());
                    response.put("activitySector", employer.getActivitySector());
                    response.put("size", employer.getSize());
                    response.put("address", employer.getAddress());
                    response.put("addressComplement", employer.getAddressComplement());
                    response.put("department", employer.getDepartment());
                    response.put("country", employer.getCountry());
                    response.put("website", employer.getWebsite());
                    response.put("firstName", employer.getFirstName());
                    response.put("lastName", employer.getLastName());
                    response.put("professionalEmail", employer.getProfessionalEmail());
                    response.put("professionalPhone", employer.getProfessionalPhone());
                    response.put("function", employer.getFunction());
                    response.put("accountStatus", employer.getAccountStatus().toString());
                });
            });
            
            logger.info("Connexion réussie pour l'utilisateur: {}", loginDto.getUsername());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (AuthenticationException e) {
            logger.warn("Échec d'authentification pour l'utilisateur: {}, cause: {}", 
                    loginDto.getUsername(), e.getMessage());
            return new ResponseEntity<>(Map.of("message", "Identifiants invalides"), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            logger.error("Erreur lors de la connexion: {}", e.getMessage());
            return new ResponseEntity<>(Map.of("message", "Erreur lors de la connexion: " + e.getMessage()), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenDto refreshTokenDto) {
        try {
            // Vérifier si le token de rafraîchissement est valide
            if (!jwtGenerator.validateToken(refreshTokenDto.getRefreshToken())) {
                return new ResponseEntity<>(Map.of("message", "Token de rafraîchissement invalide"), 
                        HttpStatus.UNAUTHORIZED);
            }

            // Extraire le nom d'utilisateur du token
            String username = jwtGenerator.getUsernameFromJWT(refreshTokenDto.getRefreshToken());

            // Charger les détails de l'utilisateur
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

            // Créer un objet Authentication
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // Générer un nouveau token
            String newToken = jwtGenerator.generateToken(authentication);

            Map<String, Object> response = new HashMap<>();
            response.put("token", newToken);
            response.put("tokenType", "Bearer");

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Erreur lors du rafraîchissement du token: {}", e.getMessage());
            return new ResponseEntity<>(Map.of("message", "Erreur lors du rafraîchissement du token"), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint pour enregistrer un employeur avec téléchargement de fichiers.
     * Garantit l'atomicité des opérations : si une partie échoue, aucune donnée n'est enregistrée.
     */
    @PostMapping(value = "/register-with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerWithFiles(@ModelAttribute MultipartRegisterRequestDTO registerDto) {
        // 1. Vérifications préalables avant toute création
        try {
            // 1.1 Vérifier si l'email existe déjà dans la table des utilisateurs
            if (userRepository.existsByUsername(registerDto.getProfessionalEmail())) {
                return new ResponseEntity<>(Map.of("message", "Cet email est déjà utilisé"), HttpStatus.BAD_REQUEST);
            }
            
            // 1.2 Vérifier si le numéro d'enregistrement existe déjà
            if (employerRepository.findByProfessionalEmail(registerDto.getProfessionalEmail()).isPresent()) {
                return new ResponseEntity<>(Map.of("message", "Ce numéro d'enregistrement est déjà utilisé"), 
                        HttpStatus.BAD_REQUEST);
            }
            
            // 1.3 Vérifier si l'email existe déjà dans la table des professionnels
            if (employerRepository.findByProfessionalEmail(registerDto.getProfessionalEmail()).isPresent()) {
                return new ResponseEntity<>(Map.of("message", "Cet email est déjà associé à un professionnel"), 
                        HttpStatus.BAD_REQUEST);
            }
            
            // 1.4 Vérification des documents obligatoires

            // 2. Préparation des objets et récupération des rôles
            Role professionalRole = roleRepository.findByName("EMPLOYER")
                    .orElseGet(() -> {
                        Role newRole = new Role("EMPLOYER");
                        return roleRepository.save(newRole);
                    });
            
            // Créer l'employeur
            Employer employer = new Employer();
            employer.setCompanyName(registerDto.getCompanyName());
            employer.setNinea(registerDto.getNinea());
            employer.setActivitySector(Employer.ActivitySector.valueOf(registerDto.getActivitySector()));
            employer.setSize(Employer.Size.valueOf(registerDto.getSize()));
            employer.setAddress(registerDto.getAddress());
            employer.setAddressComplement(registerDto.getAddressComplement());
            employer.setDepartment(Employer.department.valueOf(registerDto.getDepartment()));
            employer.setCountry(registerDto.getCountry());
            employer.setWebsite(registerDto.getWebsite());
            employer.setFirstName(registerDto.getFirstName());
            employer.setLastName(registerDto.getLastName());
            employer.setProfessionalEmail(registerDto.getProfessionalEmail());
            employer.setProfessionalPhone(registerDto.getProfessionalPhone());
            employer.setProfessionalPhoneFixed(registerDto.getProfessionalPhoneFixed());
            employer.setFunction(registerDto.getFunction());
            employer.setPassword(registerDto.getPassword());
            employer.setAccountStatus(Employer.AccountStatus.PENDING_ACTIVATION);
            employer.setStatusChangeReason("Inscription initiale en attente d'activation");
            employer.setStatusChangeDate(java.time.LocalDateTime.now());

            // 3. Commencer une transaction manuelle pour assurer l'atomicité
            org.springframework.transaction.TransactionDefinition txDef = 
                new org.springframework.transaction.support.DefaultTransactionDefinition();
            
            org.springframework.transaction.TransactionStatus txStatus = transactionManager.getTransaction(txDef);
            
            try {
                // 3.1 Sauvegarder le professionnel pour obtenir son ID
                Employer savedEmployer = employerRepository.save(employer);
                String employerId = savedEmployer.getId().toString();
                
                // 3.2 Stocker les fichiers obligatoires
  
                // 3.3 Stocker les fichiers optionnels
                if (registerDto.getNINEADocument() != null && !registerDto.getNINEADocument().isEmpty()) {
                    String nineaPath = fileStorageService.storeFile(registerDto.getNINEADocument(), "ninea", employerId);
                    savedEmployer.setNINEADocumentPath(nineaPath);
                }
                
                if (registerDto.getRCCMDocument() != null && !registerDto.getRCCMDocument().isEmpty()) {
                    String rccmPath = fileStorageService.storeFile(registerDto.getRCCMDocument(), "rccm", employerId);
                    savedEmployer.setRCCMDocumentPath(rccmPath);
                }
                
                // 3.4 Mettre à jour l'employeur avec les chemins de fichiers
                savedEmployer = employerRepository.save(savedEmployer);
                
                // 3.5 Créer l'utilisateur (APRÈS que l'employeur est créé avec succès)
                UserEntity user = new UserEntity();
                user.setUsername(registerDto.getProfessionalEmail()); // L'email est utilisé comme nom d'utilisateur
                user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
                user.setRoles(Collections.singletonList(professionalRole));
                UserEntity savedUser = userRepository.save(user);
                
                // 3.6 Si tout s'est bien passé, valider la transaction
                transactionManager.commit(txStatus);
                
                // 4. Générer le token pour l'utilisateur inscrit
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(registerDto.getProfessionalEmail(), registerDto.getPassword()));
                String token = jwtGenerator.generateToken(authentication);
                String refreshToken = jwtGenerator.generateRefreshToken(authentication);
                
                // 5. Journaliser l'inscription réussie
                logger.info("Nouveau professionnel enregistré: {} {} ({})", 
                        registerDto.getFirstName(), 
                        registerDto.getLastName(),
                        registerDto.getProfessionalEmail());
        
                // 6. Préparer la réponse
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Employeur enregistré avec succès. Vos documents seront vérifiés par notre équipe.");
                response.put("token", token);
                response.put("refreshToken", refreshToken);
                response.put("tokenType", "Bearer");
                response.put("accountStatus", employer.getAccountStatus().toString());
                response.put("employerId", savedEmployer.getId());
                
                return new ResponseEntity<>(response, HttpStatus.CREATED);
                
            } catch (Exception e) {
                // En cas d'erreur, annuler la transaction
                transactionManager.rollback(txStatus);
                logger.error("Erreur pendant la transaction: {}", e.getMessage(), e);
                return new ResponseEntity<>(Map.of("message", "Erreur lors de l'enregistrement: " + e.getMessage()),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
        } catch (Exception e) {
            // Log détaillé de l'erreur pour le débogage
            logger.error("Erreur lors de la validation: {}", e.getMessage(), e);
            return new ResponseEntity<>(Map.of("message", "Erreur lors de l'enregistrement: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Vérifie si un administrateur existe dans le système
     * @return Statut indiquant si un administrateur existe
     */
    /*@GetMapping("/admin-exists")
    public ResponseEntity<?> checkAdminExists() {
        boolean adminExists = adminService.adminExists();
        Map<String, Object> response = new HashMap<>();
        response.put("adminExists", adminExists);
        return ResponseEntity.ok(response);
    }*/

    /**
     * Endpoint pour créer le premier administrateur si aucun n'existe encore
     * Cet endpoint est uniquement disponible si aucun administrateur n'existe dans le système
     */
    /*@PostMapping("/register-first-admin")
    public ResponseEntity<?> registerFirstAdmin(@Valid @RequestBody AdminRegisterDto adminDto) {
        try {
            // Vérifier si un administrateur existe déjà
            if (adminService.adminExists()) {
                return new ResponseEntity<>(Map.of("message", "Un administrateur existe déjà dans le système"), 
                        HttpStatus.FORBIDDEN);
            }
            
            // Créer l'administrateur
            UserEntity admin = adminService.createAdmin(adminDto);
            
            // Authentifier le nouvel administrateur
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(adminDto.getEmail(), adminDto.getPassword()));
            
            // Générer les tokens
            String token = jwtGenerator.generateToken(authentication);
            String refreshToken = jwtGenerator.generateRefreshToken(authentication);
            
            // Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Administrateur créé avec succès");
            response.put("token", token);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("roles", admin.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Erreur lors de la création du premier administrateur: {}", e.getMessage(), e);
            return new ResponseEntity<>(Map.of("message", "Erreur lors de la création de l'administrateur: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }*/
    
    /**
     * Endpoint pour qu'un administrateur existant puisse créer un autre administrateur
     * Cet endpoint est uniquement accessible aux utilisateurs ayant le rôle ADMIN
     */
    /*@PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody AdminRegisterDto adminDto, Authentication authentication) {
        try {
            // Vérifier que l'utilisateur est authentifié et a le rôle ADMIN
            if (authentication == null || !authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return new ResponseEntity<>(Map.of("message", "Accès non autorisé"), 
                        HttpStatus.FORBIDDEN);
            }
            
            // Créer l'administrateur
            UserEntity admin = adminService.createAdmin(adminDto);
            
            // Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Administrateur créé avec succès");
            response.put("email", admin.getUsername());
            response.put("roles", admin.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Erreur lors de la création d'un administrateur: {}", e.getMessage(), e);
            return new ResponseEntity<>(Map.of("message", "Erreur lors de la création de l'administrateur: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }*/
}