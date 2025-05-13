package com.solutionrh.dto;

import java.time.LocalDateTime;

import org.hibernate.validator.constraints.URL;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
public class EmployerDto {
    private Long id;

    // Information sur l'entreprise
    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String companyName;

    @NotBlank(message = "Le NINEA est obligatoire")
    @Column(unique = true)
    private String ninea; // Numéro de NINEA unique

    /*
     * Mettre une liste déroulante avec ce découpage suivant :
     */
    public enum ActivitySector {
        AGRICULTURE,
        ELEVAGE_PÊCHE,
        SYLVICULTURE,
        INDUSTRIES_EXTRACTIVES,
        INDUSTRIE_MANUFACTURIERE,
        ENERGIE,
        EAU_ASSAINISSEMENT,
        BTP,
        COMMERCE,
        TRANSPORTS,
        TELECOMMUNICATIONS,
        TECHNOLOGIES_INFORMATIONS,
        FINANCE,
        IMMOBILIER,
        CONSEIL,
        TOURISME,
        RESTAURATION,
        EDUCATION,
        SANTE,
        ADMINISTRATION,
        CULTURE,
        SERVICES_PERSONNELS,
        ORGANISATIONS_NON_GOUVERNEMENTALES,
        ECONOMIE_INFORMELLE
    }

    @NotNull
    private ActivitySector activitySector;

    /*
     * Mettre une liste déroulante avec ce découpage suivant :
     * 1. Microentreprises (TPE - Très Petites Entreprises)
     * Nombre de salariés : 0 à 9
     * Caractéristiques : souvent dirigées par un seul entrepreneur, peu de
     * formalisation, structure simple.
     * 2. Petites entreprises
     * Nombre de salariés : 10 à 49
     * Caractéristiques : structure plus organisée, souvent avec des fonctions
     * spécialisées (comptabilité, RH…).
     * 3. Moyennes entreprises
     * Nombre de salariés : 50 à 249
     * Caractéristiques : organisation plus développée, potentiellement multi-sites,
     * début d'internationalisation possible.
     * 4. Grandes entreprises
     * Nombre de salariés : 250 et plus
     * Caractéristiques : forte structuration, services internes spécialisés,
     * ressources importantes, souvent multinationales.
     */
    public enum Size {
        TPE,
        PETITE_ENTREPRISE,
        MOYENNE_ENTREPRISE,
        GRANDE_ENTREPRISE
    }

    @NotNull
    private Size size;

    // Informations d'adresse de l'entreprise
    private String address; // Rue et numéro
    // Complément d'adresse
    /*
     * ajouter le département sous forme de liste déroulante
     */

    public enum department {
        DAKAR,
        GUEDIAWAYE,
        PIKINE,
        RUFISQUE,
        THIES,
        MBOUR,
        TIVAOUANE,
        DIOURBEL,
        BAMBEY,
        MBACKE,
        SAINT_LOUIS,
        DAGANA,
        PODOR,
        LOUGA,
        KEBE,
        LINGUERE,
        MATAM,
        KANEL,
        RANEROU_FERLO,
        TAMBACOUNDA,
        KOUMPENTOUM,
        GOUDIKY,
        BAKEL,
        KEDOUGOU,
        SALEMATA,
        SARAYA,
        KAFFRINE,
        KOUNGHEUL,
        BIRKELANE,
        MALEM_HODAR,
        KAOLACK,
        GUGUINEO,
        NIORO_DU_RIP,
        FATICK,
        FOUNDIOUGNE,
        GOSAS,
        SEDHIOU,
        BOUNKILING,
        GOUDOMP,
        ZIGUINCHOR,
        BIGONA,
        OUSOUYE,
        KOLDA,
        VELINGARA,
        MEDINA_YORO_FOULAH
    }

    @NotNull
    private department department;
    private String country = "Sénégal"; // Pays (valeur par défaut)

    @URL(message = "Format d'URL invalide")
    private String website;

    // Information sur le dirigeant
    private String firstName;
    private String lastName;

    // Téléphone mobile
    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^(\\+221|00221)?(7[0-9]{8})$", message = "Format de téléphone mobile sénégalais invalide. Exemple : +221770001122 ou 770001122")
    private String professionalPhone;

    // Téléphone fixe
    @Pattern(regexp = "^(\\+221|00221)?(33[0-9]{7})$", message = "Format de téléphone fixe sénégalais invalide. Exemple : +221330001122 ou 330001122")
    private String professionalPhoneFixed;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String professionalEmail;

    private String function;

    // Informations d'authentification
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    // Documents d'inscription
    // Non obligatoires
    private String NINEADocumentPath; // NINEA (Numéro d'identification de l'entreprise)
    private String RCCMDocumentPath; // Registre du Commerce et du Crédit Mobilier

    // Statut du compte
    public enum AccountStatus {
        PENDING_ACTIVATION, // En attente d'activation
        ACTIVE, // Compte actif et utilisable
        INACTIVE // Compte inactif
    }

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;
    private String statusChangeReason; // Motif du dernier changement de statut
    private LocalDateTime statusChangeDate; // Date du dernier changement de statut
}
