package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class InvoiceDTO {
    private String name;
    private String posting_date;
    private String due_date;
    private String supplier;
    private String supplier_name;
    private String status;
    private Double total;
    private Double outstanding_amount;
    private String currency;
    private Boolean paid;
    private String payment_status; 
    private List<InvoiceItemDTO> items;
    private String credit_to;
    private String credit_to_account_type;
    private String credit_to_account_currency;    
    private Boolean on_hold;
    private String release_date;
    private String hold_comment;    
    private String company_name;
}