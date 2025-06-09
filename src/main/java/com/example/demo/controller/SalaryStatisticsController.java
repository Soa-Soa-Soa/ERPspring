package com.example.demo.controller;

import com.example.demo.dto.SalaryStatisticsDTO;
import com.example.demo.service.SalaryStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CookieValue;

import java.time.Year;
import java.util.List;
import java.util.stream.IntStream;

@Controller
public class SalaryStatisticsController {

    @Autowired
    private SalaryStatisticsService statisticsService;

    @GetMapping("/salary/statistics")
    public String showStatistics(
            @RequestParam(required = false) Integer year,
            @CookieValue("sid") String sid,
            Model model) {
        // Si l'année n'est pas spécifiée, utiliser l'année en cours
        int selectedYear = year != null ? year : Year.now().getValue();
        
        // Récupérer les statistiques pour l'année sélectionnée
        List<SalaryStatisticsDTO> statistics = statisticsService.getYearlyStatistics(selectedYear, sid);
        
        // Générer la liste des années disponibles (5 ans en arrière jusqu'à l'année en cours)
        int currentYear = Year.now().getValue();
        List<Integer> availableYears = IntStream.rangeClosed(currentYear - 4, currentYear)
                .boxed().sorted((a, b) -> b.compareTo(a)).toList();

        model.addAttribute("statistics", statistics);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("availableYears", availableYears);
        
        return "salary/statistics";
    }
} 