package com.solutionrh.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.validator.constraints.URL;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.solutionrh.security.model.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.persistence.EnumType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "employer")
public class Employer implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Information sur l'entreprise
    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String companyName;

    @NotBlank(message = "Le NINEA est obligatoire")
    @Column(unique = true)
    private String ninea; // Numéro de NINEA unique

    // Enums synchronisés avec EmployerDto
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
    @Enumerated(EnumType.STRING)
    private ActivitySector activitySector;

    public enum Size {
        TPE,
        PETITE_ENTREPRISE,
        MOYENNE_ENTREPRISE,
        GRANDE_ENTREPRISE
    }

    @NotNull
    @Enumerated(EnumType.STRING)
    private Size size;

    // Informations d'adresse de l'entreprise
    private String address; // Rue et numéro
    private String addressComplement; // Complément d'adresse

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
    @Enumerated(EnumType.STRING)
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
    @Column(unique = true)
    private String professionalEmail; // Utilisé comme username pour l'authentification

    private String function;

    // Champs d'authentification
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "employer_roles", joinColumns = @JoinColumn(name = "employer_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private List<Role> roles = new ArrayList<>();

    // Documents d'inscription (synchronisés avec EmployerDto)
    private String NINEADocumentPath; // NINEA (Numéro d'identification de l'entreprise)
    private String RCCMDocumentPath; // Registre du Commerce et du Crédit Mobilier

    // Statut du compte
    public enum AccountStatus {
        PENDING_ACTIVATION, // En attente d'activation
        ACTIVE, // Compte actif et utilisable
        INACTIVE // Compte inactif
    }

    @NotNull
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;
    private String statusChangeReason; // Motif du dernier changement de statut
    private LocalDateTime statusChangeDate; // Date du dernier changement de statut

    // Implémentation des méthodes de UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
        }
        return authorities;
    }

    @Override
    public String getUsername() {
        return professionalEmail; // Utiliser l'email comme identifiant principal
    }

}
