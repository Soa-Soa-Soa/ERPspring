package com.example.demo.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.Date;

@Data
public class GenerateSalaryDTO {
    @NotBlank(message = "L'employé est obligatoire")
    private String employee;
    
    @NotNull(message = "La date de début est obligatoire")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date debut;
    
    @NotNull(message = "La date de fin est obligatoire")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date fin;
    
    @Positive(message = "Le salaire doit être positif s'il est spécifié")
    private Double salaire;
    
    // Méthode pour valider que la date de fin est après la date de début
    public boolean isDateRangeValid() {
        if (debut != null && fin != null) {
            return !fin.before(debut);
        }
        return true;
    }
} 