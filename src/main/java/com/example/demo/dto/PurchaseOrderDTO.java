package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PurchaseOrderDTO {
    private String name;
    private String transaction_date;
    private String supplier;
    private String supplier_name;
    private String status;
    private Double total;
    private String currency;
    private Double per_received;   
    private Double per_billed;  
    private List<InvoiceSummaryDTO> invoices;
    private List<PurchaseOrderItemDTO> items;
}