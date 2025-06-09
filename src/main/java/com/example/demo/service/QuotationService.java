package com.example.demo.service;

import com.example.demo.dto.QuotationDTO;
import com.example.demo.dto.QuotationItemDTO;
import com.example.demo.dto.UpdatePriceRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuotationService {
    
    @Value("${erpnext.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;

    public QuotationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<QuotationDTO> getSupplierQuotations(String sid, String supplierId) {
        // Changement de l'URL pour pointer vers Supplier Quotation
        String url = baseUrl + "/api/resource/Supplier Quotation?fields=[\"*\"]" +
                    "&filters=[[\"supplier\",\"=\",\"" + supplierId + "\"]]";
        
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
    
        List<QuotationDTO> quotations = new ArrayList<>();
        
        if (response.getBody() != null && response.getBody().has("data")) {
            JsonNode data = response.getBody().get("data");
            for (JsonNode quotation : data) {
                QuotationDTO dto = new QuotationDTO();
                dto.setName(getTextValue(quotation, "name"));
                dto.setTransaction_date(getTextValue(quotation, "transaction_date"));
                dto.setSupplier(supplierId);
                dto.setSupplier_name(getTextValue(quotation, "supplier_name"));
                dto.setStatus(getTextValue(quotation, "status"));   

                if (dto.getStatus().equals("Cancelled")) {
                    JsonNode amendedFrom = quotation.get("amended_from");
                    if (amendedFrom != null && !amendedFrom.isNull() && !amendedFrom.asText().isEmpty()) {
                        dto.setStatus("Draft");
                    }
                }

                quotations.add(dto);
            }
        }
        
        return quotations;
    }
    
    public QuotationDTO getQuotationDetails(String sid, String quotationId) {
        // Changement de l'URL pour pointer vers Supplier Quotation
        String url = baseUrl + "/api/resource/Supplier Quotation/" + quotationId + "?fields=[\"*\",\"items.*\"]";
        
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
    
        QuotationDTO quotation = new QuotationDTO();
        
        if (response.getBody() != null && response.getBody().has("data")) {
            JsonNode data = response.getBody().get("data");
            quotation.setName(getTextValue(data, "name"));
            quotation.setTransaction_date(getTextValue(data, "transaction_date"));
            quotation.setSupplier(getTextValue(data, "supplier"));
            quotation.setSupplier_name(getTextValue(data, "supplier_name"));
            quotation.setStatus(getTextValue(data, "status"));
            quotation.setTotal(getDoubleValue(data, "total"));
            quotation.setCurrency(getTextValue(data, "currency"));
            quotation.setValid_till(getTextValue(data, "valid_till"));
            
            List<QuotationItemDTO> items = new ArrayList<>();
            if (data.has("items")) {
                for (JsonNode item : data.get("items")) {
                    QuotationItemDTO itemDTO = new QuotationItemDTO();
                    itemDTO.setName(getTextValue(item, "name"));
                    itemDTO.setItem_code(getTextValue(item, "item_code"));
                    itemDTO.setItem_name(getTextValue(item, "item_name"));
                    itemDTO.setQty(getDoubleValue(item, "qty"));
                    itemDTO.setStock_uom(getTextValue(item, "stock_uom"));
                    itemDTO.setRate(getDoubleValue(item, "rate"));
                    itemDTO.setAmount(getDoubleValue(item, "amount"));
                    itemDTO.setDescription(getTextValue(item, "description"));
                    itemDTO.setConversion_factor(getDoubleValue(item, "conversion_factor"));
                    items.add(itemDTO);
                }
            }

            if (quotation.getStatus().equals("Cancelled")) {
                if (data.has("amended_from")) {
                    quotation.setStatus("Draft");
                }
            }
            quotation.setItems(items);
        }
        
        return quotation;
    }

    public void updateItemPrice(String sid, String quotationId, UpdatePriceRequest request) {
        // D'abord, vérifier le statut du devis
        QuotationDTO quotation = getQuotationDetails(sid, quotationId);
        if (!"Draft".equalsIgnoreCase(quotation.getStatus())) {
            throw new RuntimeException("Le devis doit être en état 'Draft' pour pouvoir modifier les prix");
        }
    
        String url = baseUrl + "/api/resource/Supplier Quotation Item/" + request.getItemName();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> data = new HashMap<>();
        data.put("rate", request.getNewRate());
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);
        
        restTemplate.exchange(
            url,
            HttpMethod.PUT,
            entity,
            JsonNode.class
        );
    }

    public void submitQuotation(String sid, String quotationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Soumettre le devis
        ResponseEntity<JsonNode> response = restTemplate.exchange(
            baseUrl + "/api/resource/Supplier Quotation/" + quotationId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            JsonNode.class
        );

        Map<String, Object> submitData = new HashMap<>();
        submitData.put("doctype", "Supplier Quotation");
        submitData.put("doc", response.getBody().get("data").toString());

        HttpEntity<Map<String, Object>> submitEntity = new HttpEntity<>(submitData, headers);
        
        restTemplate.exchange(
            baseUrl + "/api/method/frappe.client.submit",
            HttpMethod.POST,
            submitEntity,
            JsonNode.class
        );
    }

    public void cancelQuotation(String sid, String quotationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> submitData = new HashMap<>();
        submitData.put("doctype", "Supplier Quotation");
        submitData.put("name", quotationId);

        HttpEntity<Map<String, Object>> submitEntity = new HttpEntity<>(submitData, headers);
        
        restTemplate.exchange(
            baseUrl + "/api/method/frappe.client.cancel",
            HttpMethod.POST,
            submitEntity,
            JsonNode.class
        );
    }

    private String getTextValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : null;
    }

    private Double getDoubleValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asDouble() : null;
    }

	public void save(String sid, QuotationDTO quotation) {
        String url =  baseUrl + "/api/resource/Supplier Quotation";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> data = new HashMap<>();
        data.put("naming_series", "PUR-SQTN-.YYYY.-");
        data.put("docstatus", "0");
        data.put("transaction_date", quotation.getTransaction_date());
        data.put("valid_till", quotation.getValid_till());
        data.put("supplier", quotation.getSupplier());
        data.put("status", "Draft");
        data.put("currency", quotation.getCurrency());
        data.put("company", quotation.getCompany());

        // Gestion des items
        List<Map<String, Object>> itemsList = new ArrayList<>();
        
        for (QuotationItemDTO item : quotation.getItems()) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("item_code", item.getItem_name());
            itemMap.put("qty", item.getQty());
            itemMap.put("stock_uom", "Unit");
            itemMap.put("uom", "Unit");
            itemMap.put("conversion_factor", 1);
            itemMap.put("base_rate", item.getRate());
            itemMap.put("rate", item.getRate());
            itemMap.put("amount", item.getRate() * item.getQty());
            itemMap.put("base_amount", item.getRate() * item.getQty());
            itemMap.put("warehouse", item.getWarehouse());

            System.out.println("Item : " + item.getItem_name());
            
            itemsList.add(itemMap);
        }
        
        data.put("items", itemsList);
        
        // Calcul du total
        Double total = quotation.getItems().stream()
                .mapToDouble(item -> item.getRate() * item.getQty())
                .sum();

		Double total_qty = quotation.getItems().stream()
                .mapToDouble(item -> item.getQty())
                .sum();
        
        data.put("base_total", total);
        data.put("total", total);
        data.put("total_qty", total_qty);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                JsonNode.class
            );
            
            // Vous pourriez logger la réponse ou vérifier le status code
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Erreur lors de la création du devis: " + 
                    response.getBody().toString());
            }
        } catch (RestClientException e) {
            throw new RuntimeException("Erreur lors de l'appel à l'API ERPNext", e);
        }
    }
}