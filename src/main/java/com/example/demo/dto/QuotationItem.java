package com.example.demo.dto;

import lombok.Data;

@Data
public class QuotationItem {
    private String item;
    private Double qty;
    private Double rate;
    private String warehouse;
    
}