package com.example.demo.dto;

import lombok.Data;

@Data
public class SalaryStructureAssignmentDTO {
    private String name;
    private String employee;
    private String employeeName;
    private String salaryStructure;
    private String fromDate;
    private Double base;
    private String company;
    private Integer docstatus;
    private String amendedFrom;
    private Boolean isAmended;
} 