package com.example.demo.service;

import com.example.demo.dto.SalaryDetailDTO;
import com.example.demo.dto.SalarySlipDTO;
import com.example.demo.dto.SalaryModificationCriteriaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.YearMonth;
import java.time.LocalDate;
import java.util.Date;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class SalaryService {
    
    private static final Logger logger = LoggerFactory.getLogger(SalaryService.class);
    private static final DateTimeFormatter ERPNEXT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Value("${erpnext.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SalaryService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<SalarySlipDTO> getEmployeeSalarySlips(String sid, String employeeId) {
        String url = baseUrl + "/api/resource/Salary Slip?fields=[\"*\"]" +
                    "&filters=[[\"employee\",\"=\",\"" + employeeId + "\"]]" +
                    "&order_by=\"posting_date desc\"" +
                    "&limit=0";
        
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

            List<SalarySlipDTO> salarySlips = new ArrayList<>();
            
            if (response.getBody() != null && response.getBody().has("data")) {
                JsonNode data = response.getBody().get("data");
                for (JsonNode slip : data) {
                    SalarySlipDTO dto = new SalarySlipDTO();
                    dto.setName(getTextValue(slip, "name"));
                    dto.setEmployeeId(getTextValue(slip, "employee"));
                    dto.setEmployeeName(getTextValue(slip, "employee_name"));
                    dto.setPostingDate(getTextValue(slip, "posting_date"));
                    dto.setStartDate(getTextValue(slip, "start_date"));
                    dto.setEndDate(getTextValue(slip, "end_date"));
                    dto.setTotalWorkingDays(getIntValue(slip, "total_working_days"));
                    dto.setPaymentDays(getDoubleValue(slip, "payment_days"));
                    dto.setGrossPay(getDoubleValue(slip, "gross_pay"));
                    dto.setTotalDeduction(getDoubleValue(slip, "total_deduction"));
                    dto.setNetPay(getDoubleValue(slip, "net_pay"));
                    dto.setRoundedTotal(getDoubleValue(slip, "rounded_total"));
                    dto.setStatus(getTextValue(slip, "status"));
                    dto.setCompany(getTextValue(slip, "company"));
                    dto.setCurrency(getTextValue(slip, "currency"));
                    dto.setSalaryStructure(getTextValue(slip, "salary_structure"));
                    dto.setModeOfPayment(getTextValue(slip, "mode_of_payment"));
                    
                    // Récupérer les détails des composants du salaire
                    dto.setEarnings(getSalaryDetails(slip, "earnings"));
                    dto.setDeductions(getSalaryDetails(slip, "deductions"));
                    
                    salarySlips.add(dto);
                }
            }
            
            return salarySlips;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch salary slips: " + e.getMessage());
        }
    }

    public SalarySlipDTO getSalarySlipDetails(String sid, String slipId) {
        String url = baseUrl + "/api/resource/Salary Slip/" + slipId + "?fields=[\"*\"]&limit=0";
        
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

            if (response.getBody() != null && response.getBody().has("data")) {
                JsonNode slip = response.getBody().get("data");
                SalarySlipDTO dto = new SalarySlipDTO();
                dto.setName(getTextValue(slip, "name"));
                dto.setEmployeeId(getTextValue(slip, "employee"));
                dto.setEmployeeName(getTextValue(slip, "employee_name"));
                dto.setPostingDate(getTextValue(slip, "posting_date"));
                dto.setStartDate(getTextValue(slip, "start_date"));
                dto.setEndDate(getTextValue(slip, "end_date"));
                dto.setTotalWorkingDays(getIntValue(slip, "total_working_days"));
                dto.setPaymentDays(getDoubleValue(slip, "payment_days"));
                dto.setGrossPay(getDoubleValue(slip, "gross_pay"));
                dto.setTotalDeduction(getDoubleValue(slip, "total_deduction"));
                dto.setNetPay(getDoubleValue(slip, "net_pay"));
                dto.setRoundedTotal(getDoubleValue(slip, "rounded_total"));
                dto.setStatus(getTextValue(slip, "status"));
                dto.setCompany(getTextValue(slip, "company"));
                dto.setCurrency(getTextValue(slip, "currency"));
                dto.setSalaryStructure(getTextValue(slip, "salary_structure"));
                dto.setModeOfPayment(getTextValue(slip, "mode_of_payment"));
                
                // Récupérer les détails des composants du salaire
                dto.setEarnings(getSalaryDetails(slip, "earnings"));
                dto.setDeductions(getSalaryDetails(slip, "deductions"));
                
                return dto;
            }
            
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch salary slip details: " + e.getMessage());
        }
    }

    public List<SalarySlipDTO> getMonthlySalarySlips(String sid, YearMonth month) {
        // Formater les dates pour le filtre
        String startDate = month.atDay(1).format(DateTimeFormatter.ISO_DATE);
        String endDate = month.atEndOfMonth().format(DateTimeFormatter.ISO_DATE);
        
        String url = baseUrl + "/api/resource/Salary Slip" +
                    "?fields=[\"name\",\"employee\",\"employee_name\",\"posting_date\"," +
                    "\"start_date\",\"end_date\",\"total_working_days\",\"payment_days\"," +
                    "\"gross_pay\",\"total_deduction\",\"net_pay\",\"rounded_total\"," +
                    "\"status\",\"company\",\"currency\",\"salary_structure\",\"mode_of_payment\"]" +
                    "&filters=[[\"start_date\",\"=\",\"" + startDate + "\"]]" +
                    "&filters=[[\"end_date\",\"=\",\"" + endDate + "\"]]" +
                    "&order_by=\"posting_date desc\"" +
                    "&limit=0";
        
        logger.info("Fetching monthly salary slips with URL: {}", url);
        
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

            List<SalarySlipDTO> salarySlips = new ArrayList<>();
            
            if (response.getBody() != null && response.getBody().has("data")) {
                JsonNode data = response.getBody().get("data");
                logger.info("Received data: {}", data.toString());
                
                if (data.size() == 0) {
                    logger.warn("No salary slips found for period {} to {}", startDate, endDate);
                }
                
                for (JsonNode slip : data) {
                    SalarySlipDTO dto = new SalarySlipDTO();
                    dto.setName(getTextValue(slip, "name"));
                    dto.setEmployeeId(getTextValue(slip, "employee"));
                    dto.setEmployeeName(getTextValue(slip, "employee_name"));
                    dto.setPostingDate(getTextValue(slip, "posting_date"));
                    dto.setStartDate(getTextValue(slip, "start_date"));
                    dto.setEndDate(getTextValue(slip, "end_date"));
                    dto.setTotalWorkingDays(getIntValue(slip, "total_working_days"));
                    dto.setPaymentDays(getDoubleValue(slip, "payment_days"));
                    dto.setGrossPay(getDoubleValue(slip, "gross_pay"));
                    dto.setTotalDeduction(getDoubleValue(slip, "total_deduction"));
                    dto.setNetPay(getDoubleValue(slip, "net_pay"));
                    dto.setRoundedTotal(getDoubleValue(slip, "rounded_total"));
                    dto.setStatus(getTextValue(slip, "status"));
                    dto.setCompany(getTextValue(slip, "company"));
                    dto.setCurrency(getTextValue(slip, "currency"));
                    dto.setSalaryStructure(getTextValue(slip, "salary_structure"));
                    dto.setModeOfPayment(getTextValue(slip, "mode_of_payment"));
                    
                    // Fetch detailed salary slip information including components
                    SalarySlipDTO detailedSlip = getSalarySlipDetails(sid, dto.getName());
                    if (detailedSlip != null) {
                        dto.setEarnings(detailedSlip.getEarnings());
                        dto.setDeductions(detailedSlip.getDeductions());
                    }
                    
                    salarySlips.add(dto);
                    logger.debug("Added salary slip for employee: {} with {} earnings and {} deductions", 
                        dto.getEmployeeName(),
                        dto.getEarnings() != null ? dto.getEarnings().size() : 0,
                        dto.getDeductions() != null ? dto.getDeductions().size() : 0);
                }
            } else {
                logger.warn("No salary slips found or invalid response format");
                if (response.getBody() != null) {
                    logger.warn("Response body: {}", response.getBody().toString());
                }
            }
            
            return salarySlips;
        } catch (Exception e) {
            logger.error("Failed to fetch salary slips: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch salary slips: " + e.getMessage());
        }
    }

    private List<SalaryDetailDTO> getSalaryDetails(JsonNode slip, String type) {
        List<SalaryDetailDTO> details = new ArrayList<>();
        if (slip.has(type) && slip.get(type).isArray()) {
            logger.info("Processing {} components", type);
            for (JsonNode detail : slip.get(type)) {
                logger.info("Component data: {}", detail.toString());
                SalaryDetailDTO dto = new SalaryDetailDTO();
                dto.setComponent(getTextValue(detail, "salary_component"));
                dto.setAmount(getDoubleValue(detail, "amount"));
                dto.setDependsOnPaymentDays(getBooleanValue(detail, "depends_on_payment_days"));
                dto.setStatisticalComponent(getBooleanValue(detail, "statistical_component"));
                dto.setDoNotIncludeInTotal(getBooleanValue(detail, "do_not_include_in_total"));
                dto.setDefaultAmount(getDoubleValue(detail, "default_amount"));
                dto.setAdditionalSalary(getTextValue(detail, "additional_salary"));
                dto.setCondition(getTextValue(detail, "condition"));
                dto.setFormula(getTextValue(detail, "formula"));
                details.add(dto);
                logger.info("Added component: {} with amount: {}", dto.getComponent(), dto.getAmount());
            }
        } else {
            logger.warn("No {} data found in salary slip or data is not an array", type);
            if (slip.has(type)) {
                logger.warn("{} data type: {}", type, slip.get(type).getNodeType());
            }
        }
        return details;
    }

    private String getTextValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : null;
    }

    private Double getDoubleValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asDouble() : null;
    }

    private Integer getIntValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asInt() : null;
    }

    private Boolean getBooleanValue(JsonNode node, String fieldName) {
        if (node.has(fieldName)) {
            if (node.get(fieldName).isNumber()) {
                return node.get(fieldName).asInt() == 1;
            }
            return node.get(fieldName).asBoolean();
        }
        return null;
    }

    public List<SalarySlipDTO> generateMonthlySalarySlips(String sid, String employeeId, Date startDate, Date endDate, Double baseSalary) {
        List<SalarySlipDTO> generatedSlips = new ArrayList<>();
        
        // Convert Date to LocalDate
        LocalDate start = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        
        // Get employee info
        JsonNode employeeInfo = getEmployeeInfo(employeeId, sid);
        String employeeName = employeeInfo.get("data").get("employee_name").asText();
        String name = employeeInfo.get("data").get("name").asText();
        String company = employeeInfo.get("data").get("company").asText();
        
        // Check if employee has a salary structure assignment before proceeding
        JsonNode currentAssignment = getCurrentSalaryStructureAssignment(sid, name, start);
        if (currentAssignment == null) {
            throw new RuntimeException(
                "Impossible de générer les fiches de paie : aucune structure salariale n'est assignée à l'employé " + 
                employeeName + ". Veuillez d'abord assigner une structure salariale dans ERPNext."
            );
        }
        
        // If no base salary provided, get the last one from salary structure assignment
        Double effectiveBaseSalary = baseSalary;
        if (effectiveBaseSalary == null) {
            effectiveBaseSalary = currentAssignment.has("base") ? 
                currentAssignment.get("base").asDouble() : null;
            
            if (effectiveBaseSalary == null) {
                throw new RuntimeException("Aucun salaire de base trouvé pour l'employé. Veuillez spécifier un salaire de base.");
            }
            logger.info("Using last base salary: {} for employee {}", effectiveBaseSalary, employeeId);
        }
        
        // Generate slips for each month
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            try {
                // Calculate month start and end dates
                LocalDate monthStart = currentDate.withDayOfMonth(1);
                LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

                // Check if salary slip already exists for this month
                if (salarySlipExistsForMonth(sid, employeeId, monthStart)) {
                    logger.info("Salary slip already exists for {} - {}, skipping...", employeeId, monthStart);
                    currentDate = currentDate.plusMonths(1);
                    continue;
                }

                // Update salary structure assignment for current month
                updateSalaryStructureAssignment(sid, employeeId, currentDate, effectiveBaseSalary, company);
                
                // Create salary slip for current month
                String url = baseUrl + "/api/resource/Salary Slip";
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("Cookie", "sid=" + sid);
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                // Create salary slip document
                ObjectNode salarySlipNode = objectMapper.createObjectNode()
                    .put("doctype", "Salary Slip")
                    .put("employee", employeeId)
                    .put("employee_name", employeeName)
                    .put("company", company)
                    .put("posting_date", LocalDate.now().format(ERPNEXT_DATE_FORMATTER))
                    .put("start_date", monthStart.format(ERPNEXT_DATE_FORMATTER))
                    .put("end_date", monthEnd.format(ERPNEXT_DATE_FORMATTER))
                    .put("currency", "USD")
                    .put("exchange_rate", 1.0)
                    .put("payroll_frequency", "Monthly")
                    .put("base", effectiveBaseSalary)
                    .put("docstatus", 1);

                // Create request entity
                HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(salarySlipNode), headers);
                
                // Send request to create salary slip
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    JsonNode.class
                );
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    // Get the created salary slip details
                    String slipId = response.getBody().get("data").get("name").asText();
                    SalarySlipDTO slip = getSalarySlipDetails(sid, slipId);
                    generatedSlips.add(slip);
                    
                    logger.info("Generated salary slip for {} for month {}", employeeName, monthStart);
                } else {
                    logger.error("Failed to generate salary slip for {} for month {}", employeeName, monthStart);
                }
                
            } catch (Exception e) {
                logger.error("Error generating salary slip for month " + currentDate, e);
                throw new RuntimeException("Failed to generate salary slip for month " + currentDate + ": " + e.getMessage());
            }
            
            // Move to next month
            currentDate = currentDate.plusMonths(1);
        }
        
        return generatedSlips;
    }

    private boolean salarySlipExistsForMonth(String sid, String employeeId, LocalDate monthStart) {
        try {
            String url = baseUrl + "/api/resource/Salary Slip";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "sid=" + sid);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            
            // Build filters to check for existing salary slip
            String filters = String.format(
                "[[\"Salary Slip\",\"employee\",\"=\",\"%s\"]," +
                "[\"Salary Slip\",\"start_date\",\"=\",\"" + monthStart.format(ERPNEXT_DATE_FORMATTER) + "\"]]",
                employeeId
            );
            
            String getUrl = url + "?filters=" + filters;
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                getUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
            );
            
            return response.getBody() != null && 
                   response.getBody().has("data") && 
                   response.getBody().get("data").size() > 0;
                   
        } catch (Exception e) {
            logger.error("Error checking for existing salary slip", e);
            return false;
        }
    }

    private JsonNode getCurrentSalaryStructureAssignment(String sid, String employeeId, LocalDate date) {
        try {
            String url = baseUrl + "/api/resource/Salary Structure Assignment?fields=[\"*\"]&filters=[[\"employee\",\"=\",\"" + employeeId + "\"]]";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "sid=" + sid);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
            );
            
            if (response.getBody() != null && 
                response.getBody().has("data") && 
                response.getBody().get("data").size() > 0) {
                
                JsonNode assignment = response.getBody().get("data").get(0);
                if (assignment.has("salary_structure")) {
                    return assignment;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error getting current salary structure assignment", e);
            return null;
        }
    }

    private void updateSalaryStructureAssignment(String sid, String employeeId, LocalDate date, Double baseSalary, String company) {
        try {
            JsonNode currentAssignment = getCurrentSalaryStructureAssignment(sid, employeeId, date);
            if (currentAssignment == null) {
                throw new RuntimeException(
                    "Impossible de mettre à jour l'affectation de la structure salariale : " +
                    "aucune structure salariale n'est assignée à l'employé. " +
                    "Veuillez d'abord assigner une structure salariale dans ERPNext."
                );
            }
            
            String salaryStructure = currentAssignment.get("salary_structure").asText();
            Double lastBaseSalary = currentAssignment.has("base") ? 
                currentAssignment.get("base").asDouble() : null;
            
            // Only create new assignment if salary is different
            if (lastBaseSalary == null || !lastBaseSalary.equals(baseSalary)) {
                logger.info("Creating new salary structure assignment as base salary changed from {} to {}", lastBaseSalary, baseSalary);
                
                String url = baseUrl + "/api/resource/Salary Structure Assignment";
                HttpHeaders headers = new HttpHeaders();
                headers.set("Cookie", "sid=" + sid);
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                // Create new salary structure assignment
                ObjectNode assignmentNode = objectMapper.createObjectNode()
                    .put("doctype", "Salary Structure Assignment")
                    .put("employee", employeeId)
                    .put("salary_structure", salaryStructure)
                    .put("from_date", date.format(ERPNEXT_DATE_FORMATTER))
                    .put("company", company)
                    .put("base", baseSalary)
                    .put("docstatus", 1)
                    .put("income_tax_slab", "")
                    .put("variable_pay", 0.0);
                
                HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(assignmentNode), headers);
                
                // Create new assignment
                restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    JsonNode.class
                );
                
                logger.info("Updated salary structure assignment for employee {} with base salary {}", employeeId, baseSalary);
            } else {
                logger.info("Skipping salary structure assignment update as base salary hasn't changed ({})", baseSalary);
            }
            
        } catch (Exception e) {
            logger.error("Failed to update salary structure assignment", e);
            throw new RuntimeException("Failed to update salary structure assignment: " + e.getMessage());
        }
    }

    private JsonNode getEmployeeInfo(String employeeId, String sid) {
        String url = baseUrl + "/api/resource/Employee/" + employeeId;
        
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
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to fetch employee info");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch employee info: " + e.getMessage());
        }
    }

    private Double getLastBaseSalary(String sid, String employeeId, LocalDate date) {
        try {
            String url = baseUrl + "/api/resource/Salary Structure Assignment";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "sid=" + sid);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            
            // Get current salary structure assignment
            String filters = String.format("[[\"Salary Structure Assignment\",\"employee\",\"=\",\"%s\"],[\"Salary Structure Assignment\",\"from_date\",\"<=\",\"%s\"]]",
                employeeId, date.format(ERPNEXT_DATE_FORMATTER));
            
            String getUrl = url + "?filters=" + filters + "&order_by=from_date desc&limit=1";
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                getUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
            );
            
            if (response.getBody() != null && response.getBody().has("data") && response.getBody().get("data").size() > 0) {
                JsonNode assignment = response.getBody().get("data").get(0);
                if (assignment.has("base")) {
                    return assignment.get("base").asDouble();
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Failed to get last base salary", e);
            return null;
        }
    }

    public List<JsonNode> getSalaryComponents(String sid) {
        try {
            String url = baseUrl + "/api/resource/Salary Component?fields=[\"*\"]";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "sid=" + sid);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().get("data").findValues("name");
            }
        } catch (Exception e) {
            logger.error("Error fetching salary components", e);
        }
        return new ArrayList<>();
    }

    public List<JsonNode> getSalaryStructuresWithBaseSalary(String sid) {
        try {
            String url = baseUrl + "/api/resource/Salary Structure?fields=[\"*\"]";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "sid=" + sid);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().get("data").findValues("name");
            }
        } catch (Exception e) {
            logger.error("Error fetching salary structures", e);
        }
        return new ArrayList<>();
    }

    public void modifyBaseSalaries(String sid, SalaryModificationCriteriaDTO criteria) {
        try {
            logger.info("Starting salary modification with criteria: {}", criteria);
            
            // 1. Get all active salary structure assignments
            String url = baseUrl + "/api/resource/Salary Structure Assignment?fields=[\"name\",\"employee\",\"salary_structure\",\"base\",\"from_date\",\"company\"]&filters=[[\"docstatus\",\"=\",1]]&order_by=from_date desc&limit=1";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "sid=" + sid);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode assignments = response.getBody().get("data");
                int modifiedCount = 0;
                
                for (JsonNode assignment : assignments) {
                    String employeeId = assignment.get("employee").asText();
                    String company = assignment.get("company").asText();
                    String salaryStructure = assignment.get("salary_structure").asText();
                    double currentBase = assignment.get("base").asDouble();
                    
                    logger.info("Checking employee {} with current base salary: {}", employeeId, currentBase);

                    // 2. Check if employee's salary component meets criteria
                    if (meetsCriteria(sid, employeeId, salaryStructure, criteria)) {

                        // 3. Calculate new base salary
                        double newBase = currentBase * (1 + (criteria.getPercentageChange() / 100));
                        logger.info("Employee {} meets criteria. Creating new SSA with base from {} to {}", employeeId, currentBase, newBase);
                        
                        // 4. Déterminer la nouvelle date de début
                        LocalDate lastFromDate = LocalDate.parse(assignment.get("from_date").asText());
                        LocalDate newFromDate = lastFromDate.plusDays(1);
                        
                        // 5. Créer le nouveau SSA
                        String createUrl = baseUrl + "/api/resource/Salary Structure Assignment";
                        HttpHeaders createHeaders = new HttpHeaders();
                        createHeaders.set("Cookie", "sid=" + sid);
                        
                        createHeaders.setContentType(MediaType.APPLICATION_JSON);
                        ObjectNode assignmentNode = objectMapper.createObjectNode()
                            .put("doctype", "Salary Structure Assignment")
                            .put("employee", employeeId)
                            .put("salary_structure", salaryStructure)
                            .put("from_date", newFromDate.format(ERPNEXT_DATE_FORMATTER))
                            .put("company", company)
                            .put("base", newBase)
                            .put("docstatus", 1)
                            .put("income_tax_slab", "")
                            .put("variable_pay", 0.0);
                        
                            HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(assignmentNode), createHeaders);
                        restTemplate.exchange(
                            createUrl,
                            HttpMethod.POST,
                            requestEntity,
                            JsonNode.class
                        
                        );
                        
                        logger.info("Created new salary structure assignment for employee {} with base salary {} and from_date {}", employeeId, newBase, newFromDate);
                        modifiedCount++;
                    }
                }
                logger.info("Salary modification completed. Modified {} assignments.", modifiedCount);
            }
        } catch (Exception e) {
            logger.error("Error modifying base salaries", e);
            throw new RuntimeException("Failed to modify base salaries: " + e.getMessage());
        }
    }

    private boolean meetsCriteria(String sid, String employeeId, String salaryStructure, SalaryModificationCriteriaDTO criteria) {
        try {
            // 1. Get the latest salary slip for the employee (list, to get the name)
            String url = baseUrl + "/api/resource/Salary Slip?fields=[\"name\"]&filters=[[\"employee\",\"=\",\"" + employeeId + "\"],[\"salary_structure\",\"=\",\"" + salaryStructure + "\"]]&order_by=posting_date desc&limit=1";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "sid=" + sid);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode data = response.getBody().get("data");
                if (data.isEmpty()) {
                    logger.info("No salary slip found for employee {}", employeeId);
                    return false;
                }
                String slipName = data.get(0).get("name").asText();
                // 2. Get the full salary slip with earnings and deductions
                String slipUrl = baseUrl + "/api/resource/Salary Slip/" + slipName;
                ResponseEntity<JsonNode> slipResponse = restTemplate.exchange(
                    slipUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    JsonNode.class
                );
                if (slipResponse.getStatusCode() == HttpStatus.OK && slipResponse.getBody() != null) {
                    JsonNode slipData = slipResponse.getBody().get("data");
                    logger.info("API Response for Salary Slip Detail: {}", slipResponse.getBody().toPrettyString());
                    
                    // Normalize the criteria component name
                    String normalizedCriteriaComponent = java.text.Normalizer.normalize(
                        criteria.getSalaryComponent(), 
                        java.text.Normalizer.Form.NFC
                    );
                    
                    // Search in earnings
                    JsonNode earnings = slipData.get("earnings");
                    if (earnings != null && earnings.isArray()) {
                        for (JsonNode earning : earnings) {
                            if (earning == null) continue;
                            JsonNode component = earning.get("salary_component");
                            JsonNode amount = earning.get("amount");
                            if (component == null || amount == null) continue;
                            
                            // Normalize the component name from the API
                            String normalizedComponent = java.text.Normalizer.normalize(
                                component.asText(), 
                                java.text.Normalizer.Form.NFC
                            );
                            
                            double componentAmount = amount.asDouble();
                            logger.info("[EARNINGS] Checking component {} with amount {}", normalizedComponent, componentAmount);
                            
                            logger.info("[DEBUG] Comparing component.asText()='{}' to criteria='{}'", component.asText().trim(), criteria.getSalaryComponent().trim());
                            
                            String criteriaComponent = criteria.getSalaryComponent().replace("\"", "").trim();
                            if (component.asText().trim().equalsIgnoreCase(criteriaComponent)) {
                                boolean meetsCondition = "greater_than".equals(criteria.getOperator()) 
                                    ? componentAmount > criteria.getAmount() 
                                    : componentAmount < criteria.getAmount();
                                logger.info("[EARNINGS] Component {} meets criteria: {}", normalizedComponent, meetsCondition);
                                return meetsCondition;
                            }
                        }
                    }
                    
                    // Search in deductions
                    JsonNode deductions = slipData.get("deductions");
                    if (deductions != null && deductions.isArray()) {
                        for (JsonNode deduction : deductions) {
                            if (deduction == null) continue;
                            JsonNode component = deduction.get("salary_component");
                            JsonNode amount = deduction.get("amount");
                            if (component == null || amount == null) continue;
                            
                            // Normalize the component name from the API
                            String normalizedComponent = java.text.Normalizer.normalize(
                                component.asText(), 
                                java.text.Normalizer.Form.NFC
                            );
                            
                            double componentAmount = amount.asDouble();
                            logger.info("[DEDUCTIONS] Checking component {} with amount {}", normalizedComponent, componentAmount);
                            
                            logger.info("[DEBUG] Comparing component.asText()='{}' to criteria='{}'", component.asText().trim(), criteria.getSalaryComponent().trim());
                            
                            String criteriaComponent = criteria.getSalaryComponent().replace("\"", "").trim();
                            if (component.asText().trim().equalsIgnoreCase(criteriaComponent)) {
                                logger.info("[DEDUCTIONS] Component {} meets criteria: {}", normalizedComponent, componentAmount);
                                boolean meetsCondition = "greater_than".equals(criteria.getOperator()) 
                                    ? componentAmount > criteria.getAmount() 
                                    : componentAmount < criteria.getAmount();
                                logger.info("[DEDUCTIONS] Component {} meets criteria: {}", normalizedComponent, meetsCondition);
                                return meetsCondition;
                            }
                        }
                    }
                    logger.info("Component {} not found in earnings or deductions for employee {}", normalizedCriteriaComponent, employeeId);
                }
            }
        } catch (Exception e) {
            logger.error("Error checking salary criteria for employee {}", employeeId, e);
        }
        return false;
    }

    private void updateSalaryStructureAssignment(String sid, String assignmentName, double newBase) {
        try {
            String url = baseUrl + "/api/resource/Salary Structure Assignment/" + assignmentName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "sid=" + sid);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> data = new HashMap<>();
            data.put("base", newBase);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(data, headers);
            
            logger.info("Updating salary structure assignment {} with new base: {}", assignmentName, newBase);
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                request,
                JsonNode.class
            );
            
            if (response.getStatusCode() != HttpStatus.OK) {
                logger.error("Failed to update salary structure assignment. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to update salary structure assignment");
            }
            
            logger.info("Successfully updated salary structure assignment {}", assignmentName);
        } catch (Exception e) {
            logger.error("Error updating salary structure assignment {}", assignmentName, e);
            throw new RuntimeException("Failed to update salary structure assignment: " + e.getMessage());
        }
    }

    private void logApiResponse(String endpoint, JsonNode response) {
        try {
            logger.info("API Response for {}: {}", endpoint, response.toPrettyString());
        } catch (Exception e) {
            logger.error("Error logging API response", e);
        }
    }
} 