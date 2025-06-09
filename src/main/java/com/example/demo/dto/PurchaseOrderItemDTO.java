package com.example.demo.dto;

import lombok.Data;

@Data
public class PurchaseOrderItemDTO {
    private String name;
    private String item_code;
    private String item_name;
    private Double qty;
    private Double per_billed;
    private Double per_received;
    private Double received_qty;       // Quantité reçue
    private Double billed_qty;         // Quantité facturée
    private String stock_uom;
    private Double rate;
    private Double amount;
}