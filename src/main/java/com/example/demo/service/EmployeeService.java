package com.example.demo.service;

import com.example.demo.dto.EmployeeDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class EmployeeService {
    
    @Value("${erpnext.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    private List<String> departments = null;
    private final ObjectMapper objectMapper;

    public EmployeeService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<EmployeeDTO> getEmployees(String sid) {
        String url = baseUrl + "/api/resource/Employee?fields=[\"*\"]" +
                    "&order_by=\"employee_name asc\"" +
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

            List<EmployeeDTO> employees = new ArrayList<>();
            
            if (response.getBody() != null && response.getBody().has("data")) {
                JsonNode data = response.getBody().get("data");
                for (JsonNode employee : data) {
                    EmployeeDTO dto = new EmployeeDTO();
                    dto.setName(getTextValue(employee, "name"));
                    dto.setEmployeeName(getTextValue(employee, "employee_name"));
                    dto.setDepartment(getTextValue(employee, "department"));
                    dto.setDesignation(getTextValue(employee, "designation"));
                    dto.setStatus(getTextValue(employee, "status"));
                    dto.setCompany(getTextValue(employee, "company"));
                    dto.setUserId(getTextValue(employee, "user_id"));
                    dto.setDateOfJoining(getTextValue(employee, "date_of_joining"));
                    employees.add(dto);
                }
            }
            
            return employees;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch employees: " + e.getMessage());
        }
    }

    public List<String> getDepartments(String sid) {
        // Si les départements sont déjà en cache, les retourner
        if (departments != null) {
            return departments;
        }

        String url = baseUrl + "/api/resource/Department?fields=[\"name\"]";
        
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

            Set<String> departmentSet = new TreeSet<>(); // Pour trier automatiquement
            
            if (response.getBody() != null && response.getBody().has("data")) {
                JsonNode data = response.getBody().get("data");
                for (JsonNode dept : data) {
                    String deptName = getTextValue(dept, "name");
                    if (deptName != null && !deptName.isEmpty() && !deptName.equals("All Departments")) {
                        departmentSet.add(deptName);
                    }
                }
            }
            
            // Mettre en cache la liste des départements
            departments = new ArrayList<>(departmentSet);
            return departments;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch departments: " + e.getMessage());
        }
    }

    public void updateStatus(String employeeId, String sid, String status) {
        String url = baseUrl + "/api/resource/Employee/" + employeeId;

        if (status.equals("Inactive")) {
            status = "Active";
        } else {
            status = "Inactive";
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectNode employeeNode = objectMapper.createObjectNode()
            .put("doctype", "Employee")
            .put("name", employeeId)
            .put("status", status);

        HttpEntity<String> request = new HttpEntity<>(employeeNode.toString(), headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, request, JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la désactivation de l'employé " + employeeId + ": " + e.getMessage(), e);
        }
    }

    private String getTextValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : null;
    }
} 