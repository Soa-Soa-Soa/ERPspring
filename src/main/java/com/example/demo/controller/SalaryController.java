package com.example.demo.controller;

import com.example.demo.dto.SalarySlipDTO;
import com.example.demo.dto.GenerateSalaryDTO;
import com.example.demo.service.SalaryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;

import org.xhtmlrenderer.pdf.ITextRenderer;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.Month;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.example.demo.service.EmployeeService;
import java.util.Map;
import com.example.demo.dto.EmployeeDTO;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.demo.dto.SalaryStructureAssignmentDTO;

@Controller
@RequestMapping("/employees")
public class SalaryController {
    
    private final SalaryService salaryService;
    private final EmployeeService employeeService;
    private final SpringTemplateEngine templateEngine;

    public SalaryController(SalaryService salaryService, EmployeeService employeeService, SpringTemplateEngine templateEngine) {
        this.salaryService = salaryService;
        this.employeeService = employeeService;
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
        for (int i = 0; i <= 6; i++) {
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

    @GetMapping("/generate-salary")
    public String showGenerateSalaryForm(
            @CookieValue(name = "sid", required = true) String sid,
            Model model) {
        try {
            List<EmployeeDTO> employees = employeeService.getEmployees(sid);
            model.addAttribute("employees", employees);
            if (!model.containsAttribute("generateSalaryDTO")) {
                model.addAttribute("generateSalaryDTO", new GenerateSalaryDTO());
            }
            return "employees/generate-salary";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du chargement de la liste des employés: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/generate-salary-slips")
    public String generateSalarySlips(
            @CookieValue(name = "sid", required = true) String sid,
            @Valid @ModelAttribute("generateSalaryDTO") GenerateSalaryDTO request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        // Vérifier les erreurs de validation
        if (bindingResult.hasErrors()) {
            List<EmployeeDTO> employees = employeeService.getEmployees(sid);
            model.addAttribute("employees", employees);
            model.addAttribute("error", "Veuillez corriger les erreurs dans le formulaire");
            return "employees/generate-salary";
        }
        
        // Vérifier que la date de fin est après la date de début
        if (!request.isDateRangeValid()) {
            List<EmployeeDTO> employees = employeeService.getEmployees(sid);
            model.addAttribute("employees", employees);
            model.addAttribute("error", "La date de fin doit être après la date de début");
            return "employees/generate-salary";
        }

        try {
            salaryService.generateMonthlySalarySlips(
                sid,
                request.getEmployee(),
                request.getDebut(),
                request.getFin(),
                request.getSalaire()
            );
            redirectAttributes.addFlashAttribute("success", 
                "Les fiches de paie ont été générées avec succès pour la période du " + 
                request.getDebut() + " au " + request.getFin());
            return "redirect:/employees/" + request.getEmployee() + "/salary";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de la génération des fiches de paie: " + e.getMessage());
            return "redirect:/employees/generate-salary";
        }
    }

    @PostMapping("/{employeeId}/salary-slip/cancel")
    public String cancelSalarySlip(@PathVariable String employeeId, @RequestParam String slipId,
                                   @CookieValue(name = "sid") String sid, RedirectAttributes redirectAttributes) {
        try {
            salaryService.cancelSalarySlip(slipId, sid);
            redirectAttributes.addFlashAttribute("successMessage", "Salary Slip " + slipId + " has been cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error cancelling Salary Slip: " + e.getMessage());
        }
        // Redirect back to the details page to see the updated status
        return "redirect:/employees/" + employeeId + "/salary/details?slipId=" + slipId;
    }

    @PostMapping("/{employeeId}/salary-slip/delete")
    public String deleteSalarySlip(@PathVariable String employeeId, @RequestParam String slipId,
                                   @CookieValue(name = "sid") String sid, RedirectAttributes redirectAttributes) {
        try {
            salaryService.deleteSalarySlip(slipId, sid);
            redirectAttributes.addFlashAttribute("successMessage", "Salary Slip " + slipId + " has been deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting Salary Slip: " + e.getMessage());
        }
        
        return "redirect:/employees/" + employeeId + "/salary";
    }

    @GetMapping("/ssa/new")
    public String showSalaryStructureAssignmentForm(@CookieValue(name = "sid", required = true) String sid, @RequestParam(name = "ssaId", required = false) String ssaId, Model model) {
        SalaryStructureAssignmentDTO assignment = new SalaryStructureAssignmentDTO();

        if (ssaId != null){
            assignment = salaryService.getSSA(ssaId, sid);
            assignment.setAmendedFrom(ssaId);
        }

        System.out.println("DEBUG SSA: ssaId=" + ssaId + ", assignment.employee=" + assignment.getEmployee());

        model.addAttribute("assignment", assignment);
        model.addAttribute("employees", employeeService.getEmployees(sid));
        model.addAttribute("salaryStructures", salaryService.getSalaryStructures(sid));
        model.addAttribute("companies", employeeService.getCompanies(sid));

        return "employees/salary-structure-assignment-create";
    }

    @PostMapping("/ssa/new")
    public String createSalaryStructureAssignment(@ModelAttribute SalaryStructureAssignmentDTO assignment,
                                                  @RequestParam(value = "docstatus", required = false) Integer docstatus,
                                                  @CookieValue(name = "sid", required = true) String sid,
                                                  RedirectAttributes redirectAttributes) {
        try {
            
            JsonNode employeeInfo = salaryService.getEmployeeInfo(assignment.getEmployee(), sid);
            String company = employeeInfo.get("data").get("company").asText();
            assignment.setCompany(company);

            if (docstatus != null) {
                assignment.setDocstatus(docstatus);
            }
            else {
                assignment.setDocstatus(0);
            }

            salaryService.createSalaryStructureAssignment(assignment, sid);
            redirectAttributes.addFlashAttribute("successMessage", "Salary Structure Assignment created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create Salary Structure Assignment: " + e.getMessage());
        }
        return "redirect:/employees/ssa";
    }

    @GetMapping("/ssa")
    public String listSalaryStructureAssignments(@CookieValue(name = "sid", required = true) String sid, Model model) {
        model.addAttribute("ssaList", salaryService.getSalaryStructureAssignments(sid));
        return "employees/salary-structure-assignment-list";
    }

    @PostMapping("/ssa/{ssaId}/update/{docstatus}")
    public String updateSSA(@CookieValue(name = "sid", required = true) String sid,
                            @PathVariable(name = "docstatus", required = true) Integer docstatus,
                            @PathVariable(name = "ssaId", required = true) String ssaId,
                            RedirectAttributes redirectAttributes){
    
        try{
            salaryService.updateSSAStatus(ssaId, sid, docstatus);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update Salary Structure Assignment: " + e.getMessage());
        }

        return "redirect:/employees/ssa";
    }
} 