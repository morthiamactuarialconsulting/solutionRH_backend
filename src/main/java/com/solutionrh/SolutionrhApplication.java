package com.solutionrh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class SolutionrhApplication {

    private static final Logger logger = LoggerFactory.getLogger(SolutionrhApplication.class);

	public static void main(String[] args) {
		// Tentative de chargement du fichier .env (optionnel)
		try {
		    Dotenv dotenv = Dotenv.load();
		    // Si le fichier existe, on utilise ses valeurs
		    System.setProperty("MYSQL_USER", dotenv.get("MYSQL_USER", "dev_user"));
		    System.setProperty("MYSQL_PASSWORD", dotenv.get("MYSQL_PASSWORD", "dev_password"));
		    System.setProperty("MYSQL_HOST", dotenv.get("MYSQL_HOST", "localhost"));
		    System.setProperty("MYSQL_DATABASE", dotenv.get("MYSQL_DATABASE", "solutionrh_db"));
            logger.info("Fichier .env chargé avec succès");
		} catch (Exception e) {
		    // Si le fichier n'existe pas, on utilise les valeurs par défaut
		    logger.info("Aucun fichier .env trouvé, utilisation des valeurs par défaut");
		    // Les valeurs par défaut seront reprises depuis application-dev.properties
		}
		
		SpringApplication.run(SolutionrhApplication.class, args);
	}

}
