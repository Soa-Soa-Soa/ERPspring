package com.example.demo.service;

import com.example.demo.dto.SalaryDetailDTO;
import com.example.demo.dto.SalarySlipDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalaryService {
    
    private static final Logger logger = LoggerFactory.getLogger(SalaryService.class);
    
    @Value("${erpnext.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;

    public SalaryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<SalarySlipDTO> getEmployeeSalarySlips(String sid, String employeeId) {
        String url = baseUrl + "/api/resource/Salary Slip?fields=[\"*\"]" +
                    "&filters=[[\"employee\",\"=\",\"" + employeeId + "\"]]" +
                    "&order_by=\"posting_date desc\"" +
                    "&limit=100";
        
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
        String url = baseUrl + "/api/resource/Salary Slip/" + slipId + "?fields=[\"*\"]&limit=100";
        
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
                    "&limit=100";
        
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
} 