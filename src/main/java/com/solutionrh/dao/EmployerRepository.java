package com.solutionrh.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.solutionrh.model.Employer;


public interface EmployerRepository extends JpaRepository<Employer, Long> {
    List<Employer> findAll();
    Optional<Employer> findById(Long id);
    Optional<Employer> findByProfessionalEmail(String email);
Optional<Employer> findByNinea(String ninea);
}
