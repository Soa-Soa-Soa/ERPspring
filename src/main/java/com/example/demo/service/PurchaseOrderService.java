// src/main/java/com/example/demo/service/PurchaseOrderService.java
package com.example.demo.service;

import com.example.demo.dto.InvoiceSummaryDTO;
import com.example.demo.dto.PurchaseOrderDTO;
import com.example.demo.dto.PurchaseOrderItemDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseOrderService {
    
    @Value("${erpnext.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;

    public PurchaseOrderService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<PurchaseOrderDTO> getSupplierOrders(String sid, String supplierId) {
        // Spécifier explicitement les champs au lieu de "*"
        String url = baseUrl + "/api/resource/Purchase Order?fields=[\"name\", \"transaction_date\", \"supplier\", \"supplier_name\", \"status\", \"total\", \"currency\", \"per_received\", \"per_billed\", \"payment_schedule\"]" +
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
    
        List<PurchaseOrderDTO> orders = new ArrayList<>();
        
        if (response.getBody() != null && response.getBody().has("data")) {
            JsonNode data = response.getBody().get("data");
            for (JsonNode order : data) {
                PurchaseOrderDTO dto = new PurchaseOrderDTO();
                dto.setName(getTextValue(order, "name"));
                dto.setTransaction_date(getTextValue(order, "transaction_date"));
                dto.setSupplier(getTextValue(order, "supplier"));
                dto.setSupplier_name(getTextValue(order, "supplier_name"));
                dto.setStatus(getTextValue(order, "status"));
                dto.setTotal(getDoubleValue(order, "total"));
                dto.setCurrency(getTextValue(order, "currency"));
                dto.setPer_received(getDoubleValue(order, "per_received"));
                dto.setPer_billed(getDoubleValue(order, "per_billed"));
                
                orders.add(dto);
            }
        }
        
        return orders;
    }

    public PurchaseOrderDTO getOrderDetails(String sid, String orderId) {
        String url = baseUrl + "/api/resource/Purchase Order/" + orderId + 
                    "?fields=[\"*\"]";
        
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

        // Afficher les données reçues pour le débogage
        System.out.println("Response from ERPNext:");
        System.out.println("per_billed: " + response.getBody().get("data").get("per_billed"));
        if (response.getBody().get("data").has("items")) {
            JsonNode items = response.getBody().get("data").get("items");
            for (JsonNode item : items) {
                System.out.println("Item: " + item.get("item_code"));
                System.out.println("  qty: " + item.get("qty"));
                System.out.println("  billed_amt: " + item.get("billed_amt"));
                System.out.println("  rate: " + item.get("rate"));
                System.out.println("  amount: " + item.get("amount"));
            }
        }

        PurchaseOrderDTO order = new PurchaseOrderDTO();
        
        if (response.getBody() != null && response.getBody().has("data")) {
            JsonNode data = response.getBody().get("data");
            order.setName(getTextValue(data, "name"));
            order.setTransaction_date(getTextValue(data, "transaction_date"));
            order.setSupplier(getTextValue(data, "supplier"));
            order.setSupplier_name(getTextValue(data, "supplier_name"));
            order.setStatus(getTextValue(data, "status"));
            order.setTotal(getDoubleValue(data, "total"));
            order.setCurrency(getTextValue(data, "currency"));
            order.setPer_received(getDoubleValue(data, "per_received"));
            order.setPer_billed(getDoubleValue(data, "per_billed"));
            order.setInvoices(getLinkedInvoices(sid, orderId));

            Double perBilled = getDoubleValue(data, "per_billed");
            perBilled = perBilled != null ? perBilled / 100.0 : 0.0;
            
            List<PurchaseOrderItemDTO> items = new ArrayList<>();
            if (data.has("items")) {
                for (JsonNode item : data.get("items")) {
                    PurchaseOrderItemDTO itemDTO = new PurchaseOrderItemDTO();
                    itemDTO.setItem_code(getTextValue(item, "item_code"));
                    itemDTO.setItem_name(getTextValue(item, "item_name"));
                    Double qty = getDoubleValue(item, "qty");
                    itemDTO.setQty(qty);
                    itemDTO.setReceived_qty(getDoubleValue(item, "received_qty"));
                    
                    Double rate = getDoubleValue(item, "rate");
                    itemDTO.setRate(rate);
                    
                    // Calculer billed_qty basé sur per_billed et qty
                    if (qty != null && perBilled != null) {
                        Double billedQty = qty * perBilled;
                        itemDTO.setBilled_qty(billedQty);
                        // Calculer billed_amt basé sur billedQty et rate
                        if (rate != null) {
                            Double billedAmt = billedQty * rate;
                            itemDTO.setAmount(billedAmt);
                        }
                    } else {
                        itemDTO.setBilled_qty(0.0);
                        itemDTO.setAmount(0.0);
                    }
                    
                    itemDTO.setStock_uom(getTextValue(item, "stock_uom"));
                    itemDTO.setRate(rate);
                    items.add(itemDTO);
                }
            }
            order.setItems(items);
        }
        
        return order;
    }

    private List<InvoiceSummaryDTO> getLinkedInvoices(String sid, String orderId) {
        String url = baseUrl + "/api/resource/Purchase Invoice?fields=[\"name\",\"status\",\"outstanding_amount\",\"items.purchase_order\"]";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        List<InvoiceSummaryDTO> invoices = new ArrayList<>();
        List<InvoiceSummaryDTO> all = new ArrayList<>();
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                JsonNode.class
            );
            
            if (response.getBody() != null && 
                response.getBody().has("data") && 
                response.getBody().get("data").isArray()) {
                
                JsonNode data = response.getBody().get("data");
                for (JsonNode invoice : data) {
                    InvoiceSummaryDTO dto = new InvoiceSummaryDTO();
                    dto.setName(getTextValue(invoice, "name"));
                    dto.setStatus(getTextValue(invoice, "status"));
                    dto.setOutstanding_amount(getDoubleValue(invoice, "outstanding_amount"));
                    dto.setPurchase_order(getTextValue(invoice, "purchase_order"));
                    all.add(dto);
                }

                for (InvoiceSummaryDTO dto : all){
                    if(dto.getPurchase_order().equals(orderId)){
                        invoices.add(dto);
                    }
                }

            }
        } catch (Exception e) {
            System.err.println("Error getting invoices: " + e.getMessage());
        }
        
        return invoices;
    }

    private String getTextValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : null;
    }

    private Double getDoubleValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asDouble() : null;
    }
}