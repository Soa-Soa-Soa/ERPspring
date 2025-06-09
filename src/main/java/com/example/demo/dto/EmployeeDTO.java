package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EmployeeDTO {
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("employee_name")
    private String employeeName;
    
    @JsonProperty("department")
    private String department;
    
    @JsonProperty("designation")
    private String designation;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("company")
    private String company;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("date_of_joining")
    private String dateOfJoining;
} 