package com.solutionrh.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.solutionrh.dao.EmployerRepository;
import com.solutionrh.model.Employer;

@RestController
@RequestMapping("/api/employers")
public class EmployerController {

    private final EmployerRepository employerRepository;
    public EmployerController(EmployerRepository employerRepository) {
        this.employerRepository = employerRepository;
    }
    
    @GetMapping
    public ResponseEntity<List<Employer>> getAllEmployers() {
        List<Employer> employers = employerRepository.findAll();
        return ResponseEntity.ok(employers);
    }   
    
    @GetMapping("/{id}")
    public ResponseEntity<Employer> getEmployerById(Long id) {
        Employer employer = employerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employer non trouvé"));
        return ResponseEntity.ok(employer);
    }
}
