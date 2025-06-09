package com.example.demo.dto;

import lombok.Data;

@Data
public class InvoiceSummaryDTO {
    private String name;
    private String status;
    private Double outstanding_amount;
    private String purchase_order;
}
