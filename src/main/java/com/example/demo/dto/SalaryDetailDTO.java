package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SalaryDetailDTO {
    @JsonProperty("salary_component")
    private String component;
    
    private Double amount;
    
    @JsonProperty("depends_on_payment_days")
    private Boolean dependsOnPaymentDays;
    
    @JsonProperty("statistical_component")
    private Boolean statisticalComponent;
    
    @JsonProperty("do_not_include_in_total")
    private Boolean doNotIncludeInTotal;
    
    @JsonProperty("default_amount")
    private Double defaultAmount;
    
    @JsonProperty("additional_salary")
    private String additionalSalary;
    
    private String condition;
    private String formula;
} 