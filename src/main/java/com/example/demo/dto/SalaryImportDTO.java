package com.example.demo.dto;

import lombok.Data;

@Data
public class SalaryImportDTO {
    private String mois;
    private String refEmploye;
    private Double salaireBase;
    private String salaryStructure;
} 