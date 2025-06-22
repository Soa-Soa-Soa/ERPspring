package com.example.demo.dto;

import lombok.Data;

@Data
public class SalaryModificationCriteriaDTO {
    private String salaryComponent;  // The component to check (e.g., "indemnité")
    private String operator;         // "greater_than" or "less_than"
    private Double amount;           // The amount to compare against
    private Double percentageChange; // The percentage to modify the base salary
} 