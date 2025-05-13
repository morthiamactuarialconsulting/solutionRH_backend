package com.solutionrh.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class MultipartRegisterRequestDTO {
    
    // Information sur l'entreprise
    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String companyName;

    @NotBlank(message = "Le NINEA est obligatoire")
    private String ninea;
    
    @NotBlank(message = "Le secteur d'activité est obligatoire")
    private String activitySector;

    @NotBlank(message = "La taille de l'entreprise est obligatoire")
    private String size;

    // Adresse de l'entreprise  
    private String address;
    private String addressComplement;
    private String department;
    private String country = "Sénégal"; // Valeur par défaut
    
    private String website;

    // Information sur le recruteur
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

    // Documents à télécharger
    private MultipartFile NINEADocument; // NINEA (Numéro d'identification de l'entreprise)
    private MultipartFile RCCMDocument; // Registre du Commerce et du Crédit Mobilier
}

