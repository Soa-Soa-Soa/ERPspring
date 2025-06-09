package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class SalarySlipDTO {
    private String name;
    
    @JsonProperty("employee")
    private String employeeId;
    
    @JsonProperty("employee_name")
    private String employeeName;
    
    @JsonProperty("posting_date")
    private String postingDate;
    
    @JsonProperty("start_date")
    private String startDate;
    
    @JsonProperty("end_date")
    private String endDate;
    
    @JsonProperty("total_working_days")
    private Integer totalWorkingDays;
    
    @JsonProperty("payment_days")
    private Double paymentDays;
    
    @JsonProperty("gross_pay")
    private Double grossPay;
    
    @JsonProperty("total_deduction")
    private Double totalDeduction;
    
    @JsonProperty("net_pay")
    private Double netPay;
    
    @JsonProperty("rounded_total")
    private Double roundedTotal;
    
    private String status;
    private String company;
    private String currency;
    
    @JsonProperty("salary_structure")
    private String salaryStructure;
    
    @JsonProperty("mode_of_payment")
    private String modeOfPayment;
    
    // Détails des composants du salaire
    @JsonProperty("earnings")
    private List<SalaryDetailDTO> earnings;
    
    @JsonProperty("deductions")
    private List<SalaryDetailDTO> deductions;
} 