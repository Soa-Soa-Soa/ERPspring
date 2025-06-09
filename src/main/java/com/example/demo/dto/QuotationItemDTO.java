package com.example.demo.dto;

import lombok.Data;

@Data
public class QuotationItemDTO {
    private String name;          
    private String item_code;
    private String item_name;
    private Double qty;
    private String stock_uom;      
    private Double rate;
    private Double amount;
    private String description;    
    private Double conversion_factor; 
    private String purpose;  
    private String warehouse;
}