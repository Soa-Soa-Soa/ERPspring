package com.example.demo.controller;

import com.example.demo.dto.SalaryModificationCriteriaDTO;
import com.example.demo.service.SalaryService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/salary/modification")
@RequiredArgsConstructor
public class SalaryModificationController {

    private final SalaryService salaryService;

    @GetMapping
    public String showModificationForm(HttpServletRequest request, Model model, @CookieValue("sid") String sid) {
        List<JsonNode> salaryComponents = salaryService.getSalaryComponents(sid);
        List<JsonNode> salaryStructures = salaryService.getSalaryStructuresWithBaseSalary(sid);
        try {
            if (salaryComponents.isEmpty()) {
                model.addAttribute("error", "Aucun composant de salaire trouvé. Veuillez vérifier la configuration.");
            }
            if (salaryStructures.isEmpty()) {
                model.addAttribute("error", "Aucune structure de salaire trouvée. Veuillez vérifier la configuration.");
            }

            model.addAttribute("salaryComponents", salaryComponents);
            model.addAttribute("salaryStructures", salaryStructures);
            model.addAttribute("criteria", new SalaryModificationCriteriaDTO());
        } catch (Exception e) {
            log.error("Error loading modification form", e);
            model.addAttribute("error", "Erreur lors du chargement du formulaire: " + e.getMessage());
        }

        return "salary/modification-form";
    }

    @PostMapping
    public String processModification(
            @ModelAttribute SalaryModificationCriteriaDTO criteria,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            @CookieValue("sid") String sid) {
        
        try {
            log.info("Processing salary modification with criteria: {}", criteria);
            
            // Validate criteria
            if (criteria.getSalaryComponent() == null || criteria.getSalaryComponent().trim().isEmpty()) {
                throw new IllegalArgumentException("Le composant de salaire est requis");
            }
            if (criteria.getOperator() == null || criteria.getOperator().trim().isEmpty()) {
                throw new IllegalArgumentException("L'opérateur de comparaison est requis");
            }
            if (criteria.getAmount() == null || criteria.getAmount() <= 0) {
                throw new IllegalArgumentException("Le montant doit être supérieur à 0");
            }
            if (criteria.getPercentageChange() == null) {
                throw new IllegalArgumentException("Le pourcentage de modification est requis");
            }

            salaryService.modifyBaseSalaries(sid, criteria);
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("Les salaires de base ont été modifiés avec succès pour les employés dont le composant '%s' est %s %.2f USD.",
                    criteria.getSalaryComponent(),
                    "greater_than".equals(criteria.getOperator()) ? "supérieur à" : "inférieur à",
                    criteria.getAmount()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid criteria provided", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing salary modification", e);
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de la modification des salaires: " + e.getMessage());
        }

        return "redirect:/salary/modification";
    }
} 