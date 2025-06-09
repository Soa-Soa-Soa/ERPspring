package com.example.demo.dto;

import lombok.Data;

@Data
public class EmployeeImportDTO {
    private String ref;
    private String nom;
    private String prenom;
    private String genre;
    private String dateEmbauche;
    private String dateNaissance;
    private String company;
} 