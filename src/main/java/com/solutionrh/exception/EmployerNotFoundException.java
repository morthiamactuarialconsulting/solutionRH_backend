package com.solutionrh.exception;

public class EmployerNotFoundException extends RuntimeException {
    public EmployerNotFoundException(Long id) {
        super("Employer non trouvé avec l'ID: " + id);
    }
}
