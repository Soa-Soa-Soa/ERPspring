package com.example.demo.controller;

import com.example.demo.service.ImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    public String showImportForm() {
        return "import";
    }

    @PostMapping("/all")
    public String importAll(@RequestParam("employeesFile") MultipartFile employeesFile,
                          @RequestParam("structuresFile") MultipartFile structuresFile,
                          @RequestParam("salariesFile") MultipartFile salariesFile,
                          @CookieValue("sid") String sid,
                          Model model) {
        try {
            // Importer dans l'ordre : employés -> structures -> salaires
            importService.importEmployees(employeesFile, sid);
            importService.importSalaryStructures(structuresFile, sid);
            importService.importSalaries(salariesFile, sid);

            model.addAttribute("success", "Import réussi !");
            return "import";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de l'import : " + e.getMessage());
            return "import";
        }
    }

    @PostMapping("/erpnext/reset")
    public String resetErpnextData(@CookieValue("sid") String sid, Model model) {
        try {
            importService.resetErpnextData(sid);
            model.addAttribute("success", "Données ERPNext réinitialisées !");
            return "import";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors du reset : " + e.getMessage());
            return "import";
        }
    }

    @PostMapping("/employees")
    public ResponseEntity<?> importEmployees(@RequestParam("file") MultipartFile file,
                                           @CookieValue("sid") String sid) {
        try {
            return ResponseEntity.ok(importService.importEmployees(file, sid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error importing employees: " + e.getMessage());
        }
    }

    @PostMapping("/salary-structures")
    public ResponseEntity<?> importSalaryStructures(@RequestParam("file") MultipartFile file,
                                                  @CookieValue("sid") String sid) {
        try {
            return ResponseEntity.ok(importService.importSalaryStructures(file, sid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error importing salary structures: " + e.getMessage());
        }
    }

    @PostMapping("/salaries")
    public ResponseEntity<?> importSalaries(@RequestParam("file") MultipartFile file,
                                          @CookieValue("sid") String sid) {
        try {
            return ResponseEntity.ok(importService.importSalaries(file, sid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error importing salaries: " + e.getMessage());
        }
    }
} 