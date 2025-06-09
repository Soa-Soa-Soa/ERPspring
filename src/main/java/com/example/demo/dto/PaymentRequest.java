package com.example.demo.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String paymentDate;
    private String paymentMode;  
    private String reference;    // Référence du paiement
    private Double amount;
}
