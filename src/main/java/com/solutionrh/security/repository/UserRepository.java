package com.solutionrh.security.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.solutionrh.security.model.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    Boolean existsByUsername(String username);
    
    /**
     * Vérifie si au moins un utilisateur avec le rôle spécifié existe
     * 
     * @param roleName le nom du rôle à vérifier
     * @return true si au moins un utilisateur avec ce rôle existe, sinon false
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserEntity u JOIN u.roles r WHERE r.name = :roleName")
    boolean existsByRoleName(@Param("roleName") String roleName);
}
