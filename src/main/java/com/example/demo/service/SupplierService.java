package com.example.demo.service;

import com.example.demo.dto.SupplierDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class SupplierService {
    
    @Value("${erpnext.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;

    public SupplierService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<SupplierDTO> getSuppliers(String sid) {
        String url = baseUrl + "/api/resource/Supplier?fields=[\"*\"]";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<JsonNode> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            JsonNode.class
        );

        List<SupplierDTO> suppliers = new ArrayList<>();
        
        if (response.getBody() != null && response.getBody().has("data")) {
            JsonNode data = response.getBody().get("data");
            for (JsonNode supplier : data) {
                SupplierDTO dto = new SupplierDTO();
                dto.setName(getTextValue(supplier, "name"));
                dto.setSupplier_name(getTextValue(supplier, "supplier_name"));
                dto.setSupplier_group(getTextValue(supplier, "supplier_group"));
                dto.setSupplier_type(getTextValue(supplier, "supplier_type"));
                dto.setCountry(getTextValue(supplier, "country"));
                suppliers.add(dto);
            }
        }
        
        return suppliers;
    }

    private String getTextValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : null;
    }
}