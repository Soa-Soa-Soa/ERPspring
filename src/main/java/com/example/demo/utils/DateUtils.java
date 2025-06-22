package com.example.demo.utils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DateUtils {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM-yyyy");

    /**
     * Génère une liste de tous les mois entre deux dates au format "dd-MM-yy"
     * @param startDate Date de début au format "dd-MM-yy"
     * @param endDate Date de fin au format "dd-MM-yy"
     * @return Liste des mois au format "MM-yyyy"
     */
    public static List<String> getAllMonthsBetween(String startDate, String endDate) {
        // Parser les dates
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
        
        // Convertir en YearMonth
        YearMonth startMonth = YearMonth.from(start);
        YearMonth endMonth = YearMonth.from(end);
        
        List<String> months = new ArrayList<>();
        
        // Générer tous les mois
        YearMonth currentMonth = startMonth;
        while (!currentMonth.isAfter(endMonth)) {
            months.add(currentMonth.format(MONTH_FORMATTER));
            currentMonth = currentMonth.plusMonths(1);
        }
        
        return months;
    }

    /**
     * Vérifie si une date est au format "dd-MM-yy"
     * @param date Date à vérifier
     * @return true si le format est valide
     */
    public static boolean isValidDateFormat(String date) {
        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 