package com.example.demo.controller;

import com.example.demo.dto.SalarySlipDTO;
import com.example.demo.service.SalaryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

import org.xhtmlrenderer.pdf.ITextRenderer;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.Month;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.http.MediaType;

@Controller
@RequestMapping("/employees")
public class SalaryController {
    
    private final SalaryService salaryService;
    private final SpringTemplateEngine templateEngine;

    public SalaryController(SalaryService salaryService, SpringTemplateEngine templateEngine) {
        this.salaryService = salaryService;
        this.templateEngine = templateEngine;
    }

    @GetMapping("/{employeeId}/salary")
    public String listEmployeeSalarySlips(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String employeeId,
            Model model) {
        
        try {
            // Récupérer la liste des fiches de paie
            List<SalarySlipDTO> salarySlips = salaryService.getEmployeeSalarySlips(sid, employeeId);
            
            // Debug: Afficher les informations sur les fiches de paie
            System.out.println("Nombre de fiches de paie : " + salarySlips.size());
            for (SalarySlipDTO slip : salarySlips) {
                System.out.println("Fiche de paie - ID: " + slip.getName());
            }
            
            // Ajouter les données au modèle
            model.addAttribute("salarySlips", salarySlips);
            model.addAttribute("employeeId", employeeId);
            
            return "employees/salary-list";
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors de la récupération des fiches de paie : " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/{employeeId}/salary/details")
    public String showSalarySlipDetails(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String employeeId,
            @RequestParam("slipId") String slipId,
            Model model) {
        
        String decodedSlipId = UriUtils.decode(slipId, StandardCharsets.UTF_8.name());
        SalarySlipDTO salarySlip = salaryService.getSalarySlipDetails(sid, decodedSlipId);
        model.addAttribute("salarySlip", salarySlip);
        model.addAttribute("employeeId", employeeId);
        
        return "employees/salary-details";
    }

    @GetMapping("/{employeeId}/salary/pdf")
    public void generateSalarySlipPDF(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String employeeId,
            @RequestParam("slipId") String slipId,
            HttpServletResponse response) throws IOException {
        
        try {
            // Décoder l'ID pour la requête à l'API
            String decodedSlipId = UriUtils.decode(slipId, StandardCharsets.UTF_8.name());
            
            // Récupérer les données
            SalarySlipDTO salarySlip = salaryService.getSalarySlipDetails(sid, decodedSlipId);
            
            // Créer le contexte Thymeleaf
            Context context = new Context();
            context.setVariable("salarySlip", salarySlip);
            
            // Générer le HTML
            String html = templateEngine.process("employees/salary-pdf", context);
            
            // Configurer le renderer PDF
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            
            // Configurer la réponse HTTP
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=\"fiche-paie-" + 
                             salarySlip.getEmployeeName().replaceAll("[^a-zA-Z0-9]", "-") + "-" + salarySlip.getStartDate() + "_" + salarySlip.getEndDate() + ".pdf\"");
            
            // Générer le PDF directement dans le flux de sortie
            renderer.createPDF(response.getOutputStream());
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Erreur lors de la génération du PDF : " + e.getMessage());
        }
    }

    @GetMapping("/salary/summary")
    public String showSalarySummary(
            @CookieValue(name = "sid", required = true) String sid,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model) {
        
        // Si l'année n'est pas spécifiée, utiliser l'année courante
        if (year == null) {
            year = YearMonth.now().getYear();
        }
        
        // Si le mois n'est pas spécifié, utiliser le mois courant
        if (month == null) {
            month = YearMonth.now().getMonthValue();
        }
        
        YearMonth selectedMonth = YearMonth.of(year, month);
        
        // Formater le mois pour l'affichage
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        String formattedMonth = selectedMonth.format(formatter);
        
        // Récupérer les fiches de paie du mois
        List<SalarySlipDTO> salarySlips = salaryService.getMonthlySalarySlips(sid, selectedMonth);
        
        // Calculer les totaux
        double totalGrossPay = salarySlips.stream()
                .mapToDouble(SalarySlipDTO::getGrossPay)
                .sum();
        double totalDeductions = salarySlips.stream()
                .mapToDouble(SalarySlipDTO::getTotalDeduction)
                .sum();
        double totalNetPay = salarySlips.stream()
                .mapToDouble(SalarySlipDTO::getNetPay)
                .sum();
        
        // Générer la liste des années (de l'année courante - 5 à l'année courante)
        List<Integer> availableYears = new ArrayList<>();
        int currentYear = YearMonth.now().getYear();
        for (int i = 0; i <= 5; i++) {
            availableYears.add(currentYear - i);
        }
        
        // Liste des mois (1 à 12)
        List<Month> availableMonths = Arrays.asList(Month.values());
        
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("formattedMonth", formattedMonth);
        model.addAttribute("availableYears", availableYears);
        model.addAttribute("availableMonths", availableMonths);
        model.addAttribute("salarySlips", salarySlips);
        model.addAttribute("totalGrossPay", totalGrossPay);
        model.addAttribute("totalDeductions", totalDeductions);
        model.addAttribute("totalNetPay", totalNetPay);
        
        return "employees/salary-summary";
    }
} 