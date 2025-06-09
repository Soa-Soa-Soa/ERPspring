package com.example.demo.dto;

import java.util.List;

import lombok.Data;

@Data
public class QuotationFormDTO {
    private String supplier;
    private String valid_till;
    private String date;
    private List<QuotationItem> items;
}
