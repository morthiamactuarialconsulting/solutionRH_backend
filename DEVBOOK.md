# DEVBOOK - SolutionRH

Ce document sert de journal de développement pour le projet SolutionRH, documentant les fonctionnalités implémentées, les décisions techniques et les évolutions du projet.

## Table des matières

1. [Système d'authentification Backend](#système-dauthentification-backend)
   - [Architecture et composants](#architecture-et-composants)
   - [Flux d'authentification](#flux-dauthentification)
   - [Points forts de sécurité](#points-forts-de-sécurité)
2. [Frontend Angular](#frontend-angular)
   - [Architecture et structure](#architecture-et-structure)
   - [Fonctionnalités implémentées](#fonctionnalités-implémentées)
   - [Flux utilisateur](#flux-utilisateur)
3. [Bonnes pratiques de développement](#bonnes-pratiques-de-développement)
   - [Standards de code](#standards-de-code)
   - [Architecture et organisation](#architecture-et-organisation)
   - [Sécurité](#sécurité)
   - [Tests](#tests)
   - [Performance](#performance)
   - [Gestion des versions et déploiement](#gestion-des-versions-et-déploiement)
4. [Prochaines étapes](#prochaines-étapes)

## Système d'authentification Backend

*Date d'implémentation: 07/05/2025*

Adaptation du système d'authentification du projet ARMA-CARE pour le projet SolutionRH, avec une priorité sur la sécurité puis la performance.

### Architecture et composants

#### 1. Modèle de données

**Employer**
- Implémentation de l'interface `UserDetails` de Spring Security
- Champs d'authentification: password, accountNonExpired, accountNonLocked, etc.
- Relation ManyToMany avec la classe `Role`
- Implémentation des méthodes de `UserDetails` (getAuthorities, getUsername, etc.)
- Configuration de professionalEmail comme identifiant d'authentification

**Role**
- Entité simple avec ID et nom du rôle
- Relation avec Employer via une table de jointure

#### 2. Filtres et sécurité

**JwtAuthenticationFilter**
- Filtre qui intercepte toutes les requêtes HTTP
- Extraction du token JWT des en-têtes (Authorization: Bearer token)
- Validation du token et chargement de l'utilisateur
- Configuration du contexte de sécurité Spring

**SecurityConfig**
- Configuration globale de la sécurité Spring
- Définition des routes protégées et publiques
- Configuration du gestionnaire d'authentification
- Encodage des mots de passe avec BCrypt

#### 3. Service d'authentification

**CustomUserDetailsService**
- Implémentation de l'interface `UserDetailsService`
- Chargement des utilisateurs depuis la base de données par email
- Vérification de l'état du compte (actif, en attente, etc.)

**AuthService**
- Logique métier pour l'authentification et l'enregistrement
- Gestion de la connexion et validation des informations
- Création de nouveaux comptes avec validation
- Attribution du rôle par défaut (ROLE_EMPLOYER)

#### 4. Gestion des JWT

**JwtGenerator**
- Génération des tokens JWT (accès et rafraîchissement)
- Validation et parsing des tokens
- Extraction des informations utilisateur et des rôles
- Sécurisation avec l'algorithme HS512

#### 5. DTOs et API REST

**DTOs**
- `LoginRequestDTO`: Pour les demandes de connexion
- `RegisterRequestDTO`: Pour les inscriptions
- `JwtResponseDTO`: Pour renvoyer les tokens et informations utilisateur

**AuthController**
- Endpoints REST pour l'authentification (/api/auth/login, /api/auth/register)
- Gestion des erreurs avec réponses HTTP appropriées
- Support du CORS pour les requêtes cross-origin

### Flux d'authentification

#### Inscription (Register)
1. Le client envoie une requête POST à `/api/auth/register` avec les informations d'inscription
2. Le serveur valide les données (email unique, NINEA unique, etc.)
3. Le mot de passe est encodé avec BCrypt
4. Un nouveau compte Employer est créé avec le statut PENDING_VERIFICATION
5. Le rôle ROLE_EMPLOYER est attribué par défaut
6. Le client doit ensuite soumettre les documents requis pour vérification

#### Connexion (Login)
1. Le client envoie une requête POST à `/api/auth/login` avec email et mot de passe
2. Le serveur authentifie l'utilisateur via AuthenticationManager
3. En cas de succès, deux tokens JWT sont générés:
   - Access Token: utilisé pour l'accès aux ressources protégées
   - Refresh Token: utilisé pour obtenir un nouveau access token sans reconnexion
4. Les tokens et les informations de base de l'utilisateur sont renvoyés au client

#### Accès aux ressources protégées
1. Le client inclut l'Access Token dans l'en-tête Authorization de chaque requête
2. Le JwtAuthenticationFilter intercepte la requête et valide le token
3. Si le token est valide, l'utilisateur est chargé et le contexte de sécurité est configuré
4. La requête continue son traitement normal avec les autorisations associées

### Points forts de sécurité

- **Algorithme de signature JWT robuste**: HS512 (HMAC avec SHA-512)
- **Vérification de la longueur de clé**: minimum 512 bits pour la clé secrète
- **Encodage des mots de passe**: BCrypt avec salt aléatoire
- **Séparation des tokens**: tokens d'accès de courte durée (1h) et de rafraîchissement (24h)
- **Validation du statut des comptes**: vérification de l'état du compte avant authentification
- **Gestion des erreurs sécurisée**: pas de divulgation d'informations sensibles
- **Contrôle d'accès basé sur les rôles**: protection des routes selon les autorisations
- **Configuration CORS**: protection contre les requêtes cross-origin non autorisées

## Bonnes pratiques de développement

*Date de mise à jour: 07/05/2025*

Cette section définit les bonnes pratiques à suivre pour maintenir un code de qualité, sécurisé et performant tout au long du développement du projet SolutionRH.

### Standards de code

#### Convention de nommage

- **Packages**: Tous en minuscules, utiliser le format `com.solutionrh.<module>` (ex: `com.solutionrh.security`, `com.solutionrh.model`)
- **Classes**: PascalCase (ex: `EmployerRepository`, `JwtGenerator`)
- **Interfaces**: PascalCase, pas de préfixe "I" (ex: `UserDetails` et non `IUserDetails`)
- **Méthodes**: camelCase (ex: `findByProfessionalEmail`, `generateToken`)
- **Variables**: camelCase (ex: `employerId`, `accessToken`)
- **Constantes**: UPPER_SNAKE_CASE (ex: `JWT_EXPIRATION_TIME`)

#### Style de code

- **Indentation**: 4 espaces
- **Longueur de ligne maximale**: 120 caractères
- **JavaDoc**: Obligatoire pour les classes et méthodes publiques
- **Commentaires**: En français, clairs et concis
- **Imports**: Pas d'imports avec wildcard (`*`)
- **Lombok**: Utiliser pour réduire le boilerplate (ex: `@Data`, `@Value`)

#### Validation et qualité

- Utiliser le linter et corriger tous les avertissements avant chaque commit
- Exécuter les tests avant de soumettre du code
- Maintenir une couverture de tests d'au moins 80%
- Utiliser SonarQube pour l'analyse statique du code

### Architecture et organisation

#### Structure du projet

- Suivre le pattern MVC (Model-View-Controller)
- Respecter la séparation des responsabilités
- Organiser le code par modules fonctionnels

```
src/main/java/com/solutionrh/
  ├── controller/     # Contrôleurs REST
  ├── service/        # Services métier
  ├── dao/            # Repositories et accès aux données
  ├── model/          # Entités JPA et objets de domaine
  ├── dto/            # Objets de transfert de données
  ├── security/       # Configuration et composants de sécurité
  ├── exception/      # Exceptions personnalisées
  └── util/           # Classes utilitaires
```

#### Principes SOLID

- **S**ingle Responsibility: Chaque classe doit avoir une seule responsabilité
- **O**pen/Closed: Les classes doivent être ouvertes à l'extension mais fermées à la modification
- **L**iskov Substitution: Les classes dérivées doivent pouvoir remplacer leurs classes de base
- **I**nterface Segregation: Plusieurs interfaces spécifiques valent mieux qu'une interface générale
- **D**ependency Inversion: Dépendre des abstractions, pas des implémentations

### Sécurité

#### Principes de base

- **Defense in depth**: Mettre en place plusieurs couches de sécurité
- **Principe du moindre privilège**: Accorder uniquement les permissions nécessaires
- **Validation des entrées**: Valider toutes les entrées utilisateur (formulaires, URLs, headers)
- **Encodage des sorties**: Encoder les données avant de les renvoyer au client

#### Bonnes pratiques spécifiques

- Ne jamais stocker de mots de passe en clair, utiliser BCrypt
- Limiter les informations d'erreur exposées aux utilisateurs
- Sécuriser les tokens JWT avec des délais d'expiration courts
- Mettre en place des protections CSRF pour les opérations sensibles
- Utiliser HTTPS obligatoirement en production
- Éviter d'exposer les IDs d'entités en utilisant des identifiants publics distincts

### Tests

#### Types de tests

- **Tests unitaires**: Tester des composants isolés (services, repositories)
- **Tests d'intégration**: Tester l'interaction entre composants
- **Tests de l'API**: Tester les endpoints REST avec des scénarios complets
- **Tests de sécurité**: Tester les vulnérabilités (injection SQL, XSS, etc.)

#### Conventions de test

- Nommer les classes de test avec le suffixe `Test` (ex: `AuthServiceTest`)
- Organiser les tests selon le pattern AAA (Arrange, Act, Assert)
- Utiliser des données de test représentatives
- Mocker les dépendances externes

### Performance

- Utiliser la pagination pour les listes longues
- Optimiser les requêtes SQL avec des indexes appropriés
- Mettre en cache les données fréquemment accédées
- Utiliser des projections JPA pour limiter les données récupérées
- Éviter le chargement eager des relations JPA sauf si nécessaire
- Profiler régulièrement l'application pour identifier les goulots d'étranglement

### Gestion des versions et déploiement

#### Workflow Git

- Utiliser GitFlow comme workflow de branches
- Branches principales: `main` (production), `develop` (développement)
- Branches de fonctionnalités: `feature/nom-fonctionnalité`
- Branches de correction: `hotfix/nom-correction`
- Faire des commits atomiques avec des messages clairs et descriptifs

#### Convention de commits

Utiliser le format Conventional Commits pour les messages:

```
<type>(scope): <description>

[corps optionnel]

[footer optionnel]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

#### Déploiement

- Utiliser Maven pour la construction
- Intégration continue avec Jenkins ou GitHub Actions
- Tests automatisés avant déploiement
- Déploiement dans des environnements séparés (dev, test, prod)
- Utiliser Docker pour l'isolation et la portabilité

## Frontend Angular

*Date d'implémentation: 07/05/2025*

Implémentation d'un frontend Angular pour l'application SolutionRH, suivant l'architecture du projet ARMA-CARE-frontend, avec un focus sur les fonctionnalités d'authentification et la page d'accueil.

### Architecture et structure

#### 1. Structure du projet

```
src/
  ├── app/
  │   ├── core/                # Composants fondamentaux de l'application
  │   │   ├── models/          # Interfaces et classes de modèles
  │   │   ├── services/        # Services partagés dans toute l'application
  │   │   ├── guards/          # Guards Angular pour protection des routes
  │   │   └── interceptors/    # Intercepteurs HTTP pour la gestion des requêtes
  │   ├── features/            # Modules fonctionnels de l'application
  │   │   ├── auth/            # Fonctionnalités d'authentification
  │   │   │   ├── login/       # Composant de connexion
  │   │   │   └── register/    # Composant d'inscription
  │   │   └── home/            # Fonctionnalités de la page d'accueil
  │   │       └── landing/     # Composant de la page d'accueil
  │   └── shared/              # Composants, directives et pipes partagés
  ├── assets/                  # Ressources statiques (images, fonts, etc.)
  └── environments/            # Configuration spécifique aux environnements
```

#### 2. Modèles et interfaces

**Models**
- `LoginRequest`: Interface pour les demandes de connexion
- `RegisterRequest`: Interface pour les demandes d'inscription
- `AuthResponse`: Interface pour les réponses d'authentification
- `User`: Interface pour les données utilisateur

#### 3. Services

**AuthService**
- Gestion des fonctionnalités d'authentification
- Méthodes pour login, register, logout
- Stockage et gestion des tokens JWT
- Vérification de l'état d'authentification

#### 4. Guards et intercepteurs

**AuthGuard**
- Protection des routes nécessitant une authentification
- Redirection vers la page de connexion si non authentifié

**AuthInterceptor**
- Ajout du token JWT à chaque requête HTTP
- Gestion des erreurs 401 (non autorisé)
- Logique de rafraîchissement du token

#### 5. Composants

**LoginComponent**
- Formulaire de connexion avec validation
- Gestion des erreurs d'authentification
- Redirection après connexion réussie

**RegisterComponent**
- Formulaire d'inscription multi-étapes avec validation
- Collecte des informations utilisateur nécessaires
- Feedback utilisateur pendant le processus

**LandingComponent**
- Page d'accueil présentant SolutionRH
- Sections pour fonctionnalités, témoignages et tarification
- Appels à l'action pour l'inscription

### Fonctionnalités implémentées

#### Authentification
- Inscription utilisateur avec formulaire multi-étapes
- Connexion utilisateur avec validation
- Stockage sécurisé des tokens JWT
- Protection des routes nécessitant une authentification
- Ajout automatique des tokens aux requêtes HTTP

#### Landing Page
- Présentation des fonctionnalités principales
- Section de témoignages clients
- Affichage des options de tarification
- Formulaire de contact

### Flux utilisateur

#### Inscription
1. L'utilisateur accède à la page d'inscription
2. Remplissage du formulaire multi-étapes
3. Soumission des informations au backend
4. Redirection vers la page de connexion ou le tableau de bord

#### Connexion
1. L'utilisateur accède à la page de connexion
2. Saisie des identifiants (email professionnel et mot de passe)
3. Authentification et stockage des tokens JWT
4. Redirection vers le tableau de bord

## Prochaines étapes

### Backend
- Implémentation de l'endpoint de rafraîchissement de token (`/api/auth/refresh`)
- Gestion du mot de passe oublié (`/api/auth/forgot-password`)
- Réinitialisation du mot de passe (`/api/auth/reset-password`)
- Gestion des documents d'inscription et vérification des comptes
- Mise en place d'une rotation des clés JWT
- Ajout de logs de sécurité pour détecter les tentatives suspectes
- Limitation du nombre de tentatives de connexion (protection contre les attaques par force brute)
- Implémentation d'une authentification à deux facteurs (2FA)

### Frontend
- Implémentation du tableau de bord
- Gestion de profil utilisateur
- Fonctionnalité de mot de passe oublié
- Support pour l'authentification à deux facteurs
- Tests unitaires et d'intégration
- Optimisation des performances
- Support pour différentes tailles d'écran (responsive design)
