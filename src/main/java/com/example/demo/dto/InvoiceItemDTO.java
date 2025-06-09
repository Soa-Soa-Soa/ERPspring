package com.example.demo.dto;

import lombok.Data;

@Data
public class InvoiceItemDTO {
    private String item_code;
    private String item_name;
    private Double qty;
    private String uom;
    private Double rate;
    private Double amount;
}