package com.example.demo.service;

import com.example.demo.dto.EmployeeImportDTO;
import com.example.demo.dto.SalaryStructureImportDTO;
import com.example.demo.dto.SalaryImportDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DateTimeException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImportService {

    @Value("${erpnext.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ERPNEXT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ImportService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private void validateDate(String dateStr, String fieldName) {
        try {
            // Vérifier le format de base
            if (!dateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
                throw new RuntimeException("Format de date invalide pour " + fieldName + ". Format attendu: JJ/MM/AAAA");
            }

            String[] parts = dateStr.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            // Vérifier l'année (entre 1900 et 2100)
            if (year < 1700 || year > 2100) {
                throw new RuntimeException("Année invalide pour " + fieldName + ": " + year + ". L'année doit être entre 1900 et 2100");
            }

            // Vérifier le mois
            if (month < 1 || month > 12) {
                throw new RuntimeException("Mois invalide pour " + fieldName + ": " + month + ". Le mois doit être entre 1 et 12");
            }

            // Vérifier le jour selon le mois
            int maxDays = 31;
            if (month == 4 || month == 6 || month == 9 || month == 11) {
                maxDays = 30;
            } else if (month == 2) {
                // Gestion des années bissextiles
                if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
                    maxDays = 29;
                } else {
                    maxDays = 28;
                }
            }

            if (day < 1 || day > maxDays) {
                throw new RuntimeException("Jour invalide pour " + fieldName + ": " + day + 
                    ". Le jour doit être entre 1 et " + maxDays + " pour le mois " + month);
            }

            // Vérifier que la date peut être parsée
            LocalDate.parse(dateStr, CSV_DATE_FORMATTER);

        } catch (NumberFormatException e) {
            throw new RuntimeException("Format de date invalide pour " + fieldName + ". Les composants de la date doivent être des nombres");
        } catch (DateTimeException e) {
            throw new RuntimeException("Date invalide pour " + fieldName + ": " + e.getMessage());
        }
    }

    public List<EmployeeImportDTO> importEmployees(MultipartFile file, String sid) throws Exception {
        List<EmployeeImportDTO> employees = new ArrayList<>();
        try {
            // Créer le champ personnalisé au début de l'import
            createCustomField(sid);
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                // Skip header
                String line = reader.readLine();
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    String[] values = line.split(",");
                    EmployeeImportDTO employee = new EmployeeImportDTO();
                    employee.setRef(values[0]);
                    employee.setNom(values[1]);
                    employee.setPrenom(values[2]);
                    employee.setGenre(values[3]);
                    
                    // Valider les dates
                    try {
                        validateDate(values[4], "date d'embauche (ligne " + lineNumber + ")");
                        validateDate(values[5], "date de naissance (ligne " + lineNumber + ")");
                    } catch (RuntimeException e) {
                        rollbackEmployees(sid);
                        deleteCustomField(sid);
                        throw e;
                    }
                    
                    employee.setDateEmbauche(values[4]);
                    employee.setDateNaissance(values[5]);
                    employee.setCompany(values[6]);
                    
                    // Créer l'employé dans ERPNext
                    String employeeId = createEmployee(employee, sid);
                    
                    employees.add(employee);
                }
            }
            
            // Une fois tous les employés importés, supprimer le champ personnalisé
            deleteCustomField(sid);
            
            return employees;
        } catch (Exception e) {
            // En cas d'erreur, annuler toutes les créations
            rollbackEmployees(sid);
            deleteCustomField(sid);
            throw e;
        }
    }

    public List<SalaryStructureImportDTO> importSalaryStructures(MultipartFile file, String sid) throws Exception {
        List<SalaryStructureImportDTO> structures = new ArrayList<>();
        Map<String, List<SalaryStructureImportDTO>> structureMap = new HashMap<>();
        
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                // Skip header
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(",");
                    SalaryStructureImportDTO structure = new SalaryStructureImportDTO();
                    structure.setSalaryStructure(values[0]);
                    structure.setName(values[1]);
                    structure.setAbbr(values[2]);
                    structure.setType(values[3]);
                    structure.setValeur(values[4]);
                    structure.setCompany(values[5]);
                    
                    structureMap.computeIfAbsent(structure.getSalaryStructure(), k -> new ArrayList<>())
                              .add(structure);
                    
                    structures.add(structure);
                }
            }
            
            // Créer les structures salariales
            for (Map.Entry<String, List<SalaryStructureImportDTO>> entry : structureMap.entrySet()) {
                String structureId = createSalaryStructure(entry.getKey(), entry.getValue(), sid);
            }
            
            return structures;
        } catch (Exception e) {
            // En cas d'erreur, annuler toutes les créations
            rollbackStructures(sid);
            throw e;
        }
    }

    public List<SalaryImportDTO> importSalaries(MultipartFile file, String sid) throws Exception {
        List<SalaryImportDTO> salaries = new ArrayList<>();
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                // Skip header
                String line = reader.readLine();
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    String[] values = line.split(",");
                    SalaryImportDTO salary = new SalaryImportDTO();
                    
                    // Valider la date
                    try {
                        validateDate(values[0], "date du salaire (ligne " + lineNumber + ")");
                    } catch (RuntimeException e) {
                        rollbackSalarySlips(sid);
                        throw e;
                    }
                    
                    // Convertir le format de date pour le mois (01/04/2025 -> 2025-04-01)
                    LocalDate date = LocalDate.parse(values[0], CSV_DATE_FORMATTER);
                    salary.setMois(date.format(ERPNEXT_DATE_FORMATTER));
                    
                    salary.setRefEmploye(values[1]);
                    salary.setSalaireBase(Double.parseDouble(values[2]));
                    salary.setSalaryStructure(values[3]);
                    
                    // Créer la fiche de paie
                    String salarySlipId = createSalarySlip(salary, sid);
                    createdSalarySlipIds.add(salarySlipId);
                    
                    salaries.add(salary);
                }
            }
            return salaries;
        } catch (Exception e) {
            // En cas d'erreur, annuler toutes les créations
            rollbackSalarySlips(sid);
            throw e;
        }
    }

    private void createCustomField(String sid) {
        String url = baseUrl + "/api/resource/Custom Field";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ObjectNode customFieldNode = objectMapper.createObjectNode()
            .put("doctype", "Custom Field")
            .put("dt", "Employee")  // Le DocType cible
            .put("label", "Reference CSV")
            .put("fieldname", "reference_csv")
            .put("fieldtype", "Data")
            .put("insert_after", "employee_name")
            .put("unique", 1);
        
        HttpEntity<String> request = new HttpEntity<>(customFieldNode.toString(), headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
        } catch (Exception e) {
            // Si le champ existe déjà, on ignore l'erreur
            if (!e.getMessage().contains("already exists")) {
                throw e;
            }
        }
    }

    private void deleteCustomField(String sid) {
        String url = baseUrl + "/api/resource/Custom Field";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        // D'abord, chercher l'ID exact du champ personnalisé
        String filters = "[[\"Custom Field\",\"dt\",\"=\",\"Employee\"],[\"Custom Field\",\"fieldname\",\"=\",\"reference_csv\"]]";
        String searchUrl = url + "?filters=" + filters;
        
        HttpEntity<String> searchRequest = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(searchUrl, HttpMethod.GET, searchRequest, JsonNode.class);
            JsonNode data = response.getBody().get("data");
            if (data.size() > 0) {
                // Récupérer l'ID du champ personnalisé
                String customFieldId = data.get(0).get("name").asText();
                
                // Supprimer le champ personnalisé
                String deleteUrl = url + "/" + customFieldId;
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> deleteRequest = new HttpEntity<>(headers);
                restTemplate.exchange(deleteUrl, HttpMethod.DELETE, deleteRequest, JsonNode.class);
            }
        } catch (Exception e) {
            // Si le champ n'existe pas ou une autre erreur survient, on log simplement
            System.out.println("Erreur lors de la suppression du champ personnalisé: " + e.getMessage());
        }
    }

    private String getCompanyName(String sid){
        String url = baseUrl + "/api/resource/Company?limit=1";        
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                JsonNode.class
            );
            return response.getBody().get("data").get(0).get("name").asText();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération du nom de la société: " + e.getMessage(), e);
        }
    }

    private List<String> getAllCompanyNames(String sid) {
        String url = baseUrl + "/api/resource/Company?fields=[\"name\"]&limit=0";        
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                JsonNode.class
            );
            List<String> companies = new ArrayList<>();
            response.getBody().get("data").forEach(company -> 
                companies.add(company.get("name").asText())
            );
            return companies;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des sociétés: " + e.getMessage(), e);
        }
    }

    private void createNewCompany(String companyName, String sid) {
        String url = baseUrl + "/api/resource/Company";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Créer une abréviation à partir des premières lettres de chaque mot
        String abbr = Arrays.stream(companyName.split(" "))
            .filter(word -> !word.isEmpty())
            .map(word -> word.substring(0, 1).toUpperCase())
            .collect(Collectors.joining());

        ObjectNode companyNode = objectMapper.createObjectNode()
            .put("doctype", "Company")
            .put("company_name", companyName)
            .put("abbr", abbr)
            .put("default_currency", "USD")
            .put("country", "Madagascar")
            .put("default_holiday_list", "Holiday Test");

        HttpEntity<String> request = new HttpEntity<>(companyNode.toString(), headers);
        try {
            restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
        } catch (Exception e) {
            // Si l'entreprise existe déjà, on ignore l'erreur
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                 System.out.println("L'entreprise " + companyName + " existe déjà, l'import continue.");
            } else {
                throw new RuntimeException("Erreur lors de la création de l'entreprise " + companyName + ": " + e.getMessage(), e);
            }
        }
    }

    private String createEmployee(EmployeeImportDTO employee, String sid) {           
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Vérifier si l'entreprise existe parmi toutes les entreprises
        try {
            List<String> companies = getAllCompanyNames(sid);
            if (!companies.contains(employee.getCompany())) {
                createNewCompany(employee.getCompany(), sid);
            }
        } catch (Exception e) {
            // Si la récupération de la liste échoue, on tente quand même de créer la société.
            // La méthode createNewCompany gère le cas où elle existe déjà.
            System.out.println("Impossible de lister les sociétés, tentative de création de " + employee.getCompany() + ". Erreur: " + e.getMessage());
            createNewCompany(employee.getCompany(), sid);
        }

        String url = baseUrl + "/api/resource/Employee";

        // Convertir les dates au format ERPNext
        LocalDate dateNaissance = LocalDate.parse(employee.getDateNaissance(), CSV_DATE_FORMATTER);
        LocalDate dateEmbauche = LocalDate.parse(employee.getDateEmbauche(), CSV_DATE_FORMATTER);
        LocalDate dateRetraite = dateNaissance.plusYears(60);

        ObjectNode employeeNode = objectMapper.createObjectNode()
            .put("doctype", "Employee")
            .put("company", employee.getCompany())
            .put("first_name", employee.getPrenom())
            .put("last_name", employee.getNom())
            .put("employee_name", employee.getPrenom() + " " + employee.getNom())
            .put("gender", employee.getGenre().equals("Masculin") ? "Male" : "Female")
            .put("date_of_birth", dateNaissance.format(ERPNEXT_DATE_FORMATTER))
            .put("date_of_joining", dateEmbauche.format(ERPNEXT_DATE_FORMATTER))
            .put("status", "Active")
            .put("create_user_permission", 1)
            .put("date_of_retirement", dateRetraite.format(ERPNEXT_DATE_FORMATTER))
            .put("salary_currency", "USD")
            .put("reference_csv", employee.getRef());
        
        HttpEntity<String> request = new HttpEntity<>(employeeNode.toString(), headers);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
            String employeeId = response.getBody().get("data").get("name").asText();
            employeeRefMap.put(employee.getRef(), employeeId);
            return employeeId;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de l'employé: " + e.getMessage(), e);
        }
    }

    // Map pour stocker les correspondances entre références et IDs ERPNext
    private final Map<String, String> employeeRefMap = new HashMap<>();

    private String getErpEmployeeId(String ref, String sid) {
        // Si on a déjà la correspondance en mémoire
        if (employeeRefMap.containsKey(ref)) {
            return employeeRefMap.get(ref);
        }

        // Sinon, chercher l'employé dans ERPNext par sa référence CSV
        String url = baseUrl + "/api/resource/Employee";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // Utiliser le champ personnalisé pour la recherche
        String filters = String.format("[[\"Employee\",\"reference_csv\",\"=\",\"%s\"]]", ref);
        url += "?filters=" + filters;

        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode data = response.getBody().get("data");
            if (data.size() > 0) {
                String erpEmployeeId = data.get(0).get("name").asText();
                employeeRefMap.put(ref, erpEmployeeId);
                return erpEmployeeId;
            }
            throw new RuntimeException("Employé non trouvé avec la référence CSV: " + ref);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche de l'employé: " + e.getMessage(), e);
        }
    }

    private void createSalaryComponent(SalaryStructureImportDTO component, String sid) {
        String url = baseUrl + "/api/resource/Salary Component";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String abbr = Arrays.stream(component.getCompany().split(" "))
        .filter(word -> !word.isEmpty())
        .map(word -> word.substring(0, 1).toUpperCase())
        .collect(Collectors.joining());

        // Créer le tableau des comptes
        ArrayNode accountsArray = objectMapper.createArrayNode();
        ObjectNode accountNode = objectMapper.createObjectNode()
            .put("company", component.getCompany())
            .put("account", "Cash - " + abbr)
            .put("doctype", "Salary Component Account");
        accountsArray.add(accountNode);
        
        // Convertir le type en majuscule
        String type = component.getType();
        if(type.equals("earning")){
            type = "Earning";
        }else{
            type = "Deduction";
        }

        ObjectNode componentNode = objectMapper.createObjectNode()
            .put("doctype", "Salary Component")
            .put("salary_component", component.getName())
            .put("salary_component_abbr", component.getAbbr())
            .put("type", type)
            .put("depends_on_payment_days", 0)
            .put("is_tax_applicable", 0)
            .put("deduct_full_tax_on_selected_payroll_date", 0)
            .put("variable_based_on_taxable_salary", 0)
            .put("is_income_tax_component", 0)
            .put("exempted_from_income_tax", 0)
            .put("round_to_the_nearest_integer", 0)
            .put("statistical_component", 0)
            .put("do_not_include_in_total", 0)
            .put("remove_if_zero_valued", 1)
            .put("disabled", 0)
            .put("amount", 0.0)
            .put("amount_based_on_formula", 1)
            .put("formula", component.getValeur())
            .put("is_flexible_benefit", 0)
            .put("max_benefit_amount", 0.0)
            .put("pay_against_benefit_claim", 0)
            .put("only_tax_impact", 0)
            .put("create_separate_payment_entry_against_benefit_claim", 0);

        componentNode.set("accounts", accountsArray);
        
        HttpEntity<String> request = new HttpEntity<>(componentNode.toString(), headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
        } catch (Exception e) {
            // Si le composant existe déjà, on ignore l'erreur
            if (!e.getMessage().contains("already exists")) {
                throw e;
            }
        }
    }

    private String createSalaryStructure(String name, List<SalaryStructureImportDTO> components, String sid) {
        // 1. Créer les composants de salaire s'ils n'existent pas
        for (SalaryStructureImportDTO component : components) {
            createSalaryComponent(component, sid);
        }
        
        // 2. Vérifier que tous les composants ont été créés
        for (SalaryStructureImportDTO component : components) {
            verifyComponentExists(component.getName(), sid);
        }
        
        // 3. Créer la structure salariale
        String url = baseUrl + "/api/resource/Salary Structure";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ObjectNode structureNode = objectMapper.createObjectNode()
            .put("doctype", "Salary Structure")
            .put("name", name)
            .put("company", components.get(0).getCompany())
            .put("is_active", "Yes")
            .put("payroll_frequency", "Monthly")
            .put("currency", "USD")
            .put("docstatus", 1);
        
        // Ajouter les composants
        ArrayNode earnings = objectMapper.createArrayNode();
        ArrayNode deductions = objectMapper.createArrayNode();
        
        for (SalaryStructureImportDTO component : components) {
            // Convertir le type en majuscule
            String type = component.getType();
            if(type.equals("earning")){
                type = "Earning";
            }else{
                type = "Deduction";
            }
            
            ObjectNode componentNode = objectMapper.createObjectNode()
                .put("doctype", "Salary Detail")
                .put("salary_component", component.getName())
                .put("amount", 0.0)
                .put("amount_based_on_formula", 1)
                .put("formula", component.getValeur())
                .put("depends_on_payment_days", 0);
            
            // Comparer avec le type en majuscule
            if ("Earning".equals(type)) {
                earnings.add(componentNode);
            } else {
                deductions.add(componentNode);
            }
        }
        
        structureNode.set("earnings", earnings);
        structureNode.set("deductions", deductions);
        
        HttpEntity<String> request = new HttpEntity<>(structureNode.toString(), headers);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
            return response.getBody().get("data").get("name").asText();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de la structure salariale: " + e.getMessage(), e);
        }
    }

    private String createSalarySlip(SalaryImportDTO salary, String sid) {
        // Convertir la référence en ID ERPNext
        String erpEmployeeId = getErpEmployeeId(salary.getRefEmploye(), sid);
        
        // Vérifier que l'employé existe
        verifyEmployeeExists(erpEmployeeId, sid);
        
        // Vérifier que la structure salariale existe
        verifySalaryStructureExists(salary.getSalaryStructure(), sid);

        // Créer ou mettre à jour l'affectation de la structure salariale
        createSalaryStructureAssignment(erpEmployeeId, salary, sid);
        
        // Récupérer les composants de la structure salariale
        JsonNode structureComponents = getSalaryStructureComponents(salary.getSalaryStructure(), sid);
        
        String url = baseUrl + "/api/resource/Salary Slip";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Calculer les dates de début et fin du mois
        LocalDate startDate = LocalDate.parse(salary.getMois(), ERPNEXT_DATE_FORMATTER).withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // Récupérer les informations de l'employé
        JsonNode employeeInfo = getEmployeeInfo(erpEmployeeId, sid);
        String employeeName = employeeInfo.get("data").get("employee_name").asText();
        String company = employeeInfo.get("data").get("company").asText();
        
        ObjectNode salarySlipNode = objectMapper.createObjectNode()
            .put("doctype", "Salary Slip")
            .put("employee", erpEmployeeId)
            .put("employee_name", employeeName)
            .put("company", company)
            .put("posting_date", LocalDate.now().format(ERPNEXT_DATE_FORMATTER))
            .put("salary_structure", salary.getSalaryStructure())
            .put("start_date", startDate.format(ERPNEXT_DATE_FORMATTER))
            .put("end_date", endDate.format(ERPNEXT_DATE_FORMATTER))
            .put("currency", "USD")
            .put("exchange_rate", 1.0)
            .put("payroll_frequency", "Monthly")
            .put("base", salary.getSalaireBase())
            .put("docstatus", 1);

        // Créer les tableaux pour earnings et deductions
        ArrayNode earnings = objectMapper.createArrayNode();
        ArrayNode deductions = objectMapper.createArrayNode();

        // Ajouter les composants de la structure salariale
        if (structureComponents.has("earnings")) {
            for (JsonNode earning : structureComponents.get("earnings")) {
                ObjectNode componentNode = objectMapper.createObjectNode()
                    .put("doctype", "Salary Detail")
                    .put("salary_component", earning.get("salary_component").asText())
                    .put("abbr", earning.get("abbr").asText())
                    .put("amount", earning.get("amount").asDouble())
                    .put("default_amount", earning.get("amount").asDouble())
                    .put("amount_based_on_formula", earning.get("amount_based_on_formula").asInt())
                    .put("formula", earning.get("formula").asText())
                    .put("depends_on_payment_days", earning.get("depends_on_payment_days").asInt());
                earnings.add(componentNode);
            }
        }

        if (structureComponents.has("deductions")) {
            for (JsonNode deduction : structureComponents.get("deductions")) {
                ObjectNode componentNode = objectMapper.createObjectNode()
                    .put("doctype", "Salary Detail")
                    .put("salary_component", deduction.get("salary_component").asText())
                    .put("abbr", deduction.get("abbr").asText())
                    .put("amount", deduction.get("amount").asDouble())
                    .put("default_amount", deduction.get("amount").asDouble())
                    .put("amount_based_on_formula", deduction.get("amount_based_on_formula").asInt())
                    .put("formula", deduction.get("formula").asText())
                    .put("depends_on_payment_days", deduction.get("depends_on_payment_days").asInt());
                deductions.add(componentNode);
            }
        }

        salarySlipNode.set("earnings", earnings);
        salarySlipNode.set("deductions", deductions);
        
        HttpEntity<String> request = new HttpEntity<>(salarySlipNode.toString(), headers);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
            return response.getBody().get("data").get("name").asText();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de la fiche de paie: " + e.getMessage(), e);
        }
    }

    private void createSalaryStructureAssignment(String employeeId, SalaryImportDTO salary, String sid) {
        String url = baseUrl + "/api/resource/Salary Structure Assignment";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Récupérer les informations de l'employé pour la société
        JsonNode employeeInfo = getEmployeeInfo(employeeId, sid);
        String company = employeeInfo.get("data").get("company").asText();
        
        // Convertir la date au format ERPNext
        LocalDate startDate = LocalDate.parse(salary.getMois(), ERPNEXT_DATE_FORMATTER).withDayOfMonth(1);
        
        ObjectNode assignmentNode = objectMapper.createObjectNode()
            .put("doctype", "Salary Structure Assignment")
            .put("employee", employeeId)
            .put("salary_structure", salary.getSalaryStructure())
            .put("from_date", startDate.format(ERPNEXT_DATE_FORMATTER))
            .put("company", company)
            .put("base", salary.getSalaireBase())
            .put("docstatus", 1)  // Submit the document
            .put("income_tax_slab", "")
            .put("variable_pay", 0.0);
        
        HttpEntity<String> request = new HttpEntity<>(assignmentNode.toString(), headers);
        
        try {
            // Créer une nouvelle affectation
            restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création/mise à jour de l'affectation de la structure salariale: " + e.getMessage(), e);
        }
    }

    private String getExistingSalaryStructureAssignment(String employeeId, LocalDate date, String sid) {
        String url = baseUrl + "/api/resource/Salary Structure Assignment";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        // Construire les filtres pour trouver une affectation existante
        String filters = String.format("[[\"Salary Structure Assignment\",\"employee\",\"=\",\"%s\"],[\"Salary Structure Assignment\",\"from_date\",\"<=\",\"%s\"]]",
            employeeId, date.format(ERPNEXT_DATE_FORMATTER));
        
        url += "?filters=" + filters + "&order_by=from_date desc&limit=1";
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode data = response.getBody().get("data");
            if (data.size() > 0) {
                return data.get(0).get("name").asText();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode getSalaryStructureComponents(String structureId, String sid) {
        String url = baseUrl + "/api/resource/Salary Structure/" + structureId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode data = response.getBody().get("data");
            ObjectNode components = objectMapper.createObjectNode();
            
            if (data.has("earnings")) {
                components.set("earnings", data.get("earnings"));
            }
            if (data.has("deductions")) {
                components.set("deductions", data.get("deductions"));
            }
            
            return components;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des composants de la structure salariale: " + e.getMessage(), e);
        }
    }

    private JsonNode getEmployeeInfo(String employeeId, String sid) {
        String url = baseUrl + "/api/resource/Employee/" + employeeId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de récupérer les informations de l'employé " + employeeId, e);
        }
    }

    private void verifyEmployeeExists(String employeeId, String sid) {
        String url = baseUrl + "/api/resource/Employee/" + employeeId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException("L'employé " + employeeId + " n'existe pas", e);
        }
    }

    private void verifySalaryStructureExists(String structureId, String sid) {
        String url = baseUrl + "/api/resource/Salary Structure/" + structureId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException("La structure salariale " + structureId + " n'existe pas", e);
        }
    }

    private void verifyComponentExists(String componentId, String sid) {
        String url = baseUrl + "/api/resource/Salary Component/" + componentId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException("Le composant salarial " + componentId + " n'existe pas", e);
        }
    }

    private void deleteEmployee(String employeeId, String sid) {
        String url = baseUrl + "/api/resource/Employee/" + employeeId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, request, JsonNode.class);
        } catch (Exception e) {
            // Log l'erreur mais continue
            System.out.println("Erreur lors de la suppression de l'employé " + employeeId + ": " + e.getMessage());
        }
    }

    private void deleteSalaryStructure(String structureId, String sid) {
        String url = baseUrl + "/api/resource/Salary Structure/" + structureId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, request, JsonNode.class);
        } catch (Exception e) {
            System.out.println("Erreur lors de la suppression de la structure " + structureId + ": " + e.getMessage());
        }
    }

    private void deleteSalarySlip(String salarySlipId, String sid) {
        String url = baseUrl + "/api/resource/Salary Slip/" + salarySlipId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, request, JsonNode.class);
        } catch (Exception e) {
            System.out.println("Erreur lors de la suppression du salaire " + salarySlipId + ": " + e.getMessage());
        }
    }

    // Liste pour stocker les IDs créés pendant l'import
    private List<String> createdEmployeeIds = new ArrayList<>();
    private List<String> createdStructureIds = new ArrayList<>();
    private List<String> createdSalarySlipIds = new ArrayList<>();

    private void rollbackEmployees(String sid) {
        for (String employeeId : createdEmployeeIds) {
            deleteEmployee(employeeId, sid);
        }
        createdEmployeeIds.clear();
    }

    private void rollbackStructures(String sid) {
        for (String structureId : createdStructureIds) {
            deleteSalaryStructure(structureId, sid);
        }
        createdStructureIds.clear();
    }

    private void rollbackSalarySlips(String sid) {
        for (String salarySlipId : createdSalarySlipIds) {
            deleteSalarySlip(salarySlipId, sid);
        }
        createdSalarySlipIds.clear();
    }

    public String resetErpnextData(String sid) {
        String url = baseUrl + "/api/method/erpnext.setup.reset_data.reset_all_data";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
    
        HttpEntity<String> request = new HttpEntity<>("{}", headers);
    
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du reset des données ERPNext: " + e.getMessage(), e);
        }
    }
} 