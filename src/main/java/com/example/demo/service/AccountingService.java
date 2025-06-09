// src/main/java/com/example/demo/service/AccountingService.java
package com.example.demo.service;

import com.example.demo.dto.InvoiceDTO;
import com.example.demo.dto.InvoiceItemDTO;
import com.example.demo.dto.PaymentRequest;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountingService {
    
    @Value("${erpnext.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;

    public AccountingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<InvoiceDTO> getInvoices(String sid) {
        String url = baseUrl + "/api/resource/Purchase Invoice?fields=[\"*\"]" +
                    "&order_by=\"posting_date desc\"";
        
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

        List<InvoiceDTO> invoices = new ArrayList<>();
        
        if (response.getBody() != null && response.getBody().has("data")) {
            JsonNode data = response.getBody().get("data");
            for (JsonNode invoice : data) {
                InvoiceDTO dto = new InvoiceDTO();
                dto.setName(getTextValue(invoice, "name"));
                dto.setStatus(getTextValue(invoice, "status"));
                dto.setPosting_date(getTextValue(invoice, "posting_date"));
                dto.setDue_date(getTextValue(invoice, "due_date"));
                dto.setSupplier(getTextValue(invoice, "supplier"));
                dto.setSupplier_name(getTextValue(invoice, "supplier_name"));
                dto.setStatus(getTextValue(invoice, "status"));
                dto.setTotal(getDoubleValue(invoice, "total"));
                dto.setOutstanding_amount(getDoubleValue(invoice, "outstanding_amount"));
                dto.setCurrency(getTextValue(invoice, "currency"));

                dto.setOn_hold(invoice.has("on_hold") ? invoice.get("on_hold").asBoolean() : false);
                if (dto.getOn_hold()) {
                    dto.setStatus("On Hold");
                }
                
                
                invoices.add(dto);
            }
        }
        
        return invoices;
    }

    public InvoiceDTO getInvoiceDetails(String sid, String invoiceId) {
        String url = baseUrl + "/api/resource/Purchase Invoice/" + invoiceId + 
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

        InvoiceDTO invoice = new InvoiceDTO();
        
        if (response.getBody() != null && response.getBody().has("data")) {
            JsonNode data = response.getBody().get("data");
            invoice.setStatus(getTextValue(data, "status"));
            invoice.setName(getTextValue(data, "name"));
            invoice.setPosting_date(getTextValue(data, "posting_date"));
            invoice.setDue_date(getTextValue(data, "due_date"));
            invoice.setSupplier(getTextValue(data, "supplier"));
            invoice.setSupplier_name(getTextValue(data, "supplier_name"));
            invoice.setCompany_name(getTextValue(data, "company"));
            invoice.setStatus(getTextValue(data, "status"));
            invoice.setTotal(getDoubleValue(data, "total"));
            invoice.setOutstanding_amount(getDoubleValue(data, "outstanding_amount"));
            invoice.setCurrency(getTextValue(data, "currency"));
            invoice.setCredit_to(getTextValue(data, "credit_to"));
            invoice.setCredit_to_account_type(getTextValue(data, "credit_to_account_type"));
            invoice.setCredit_to_account_currency(getTextValue(data, "credit_to_account_currency"));
            invoice.setOn_hold(data.has("on_hold") ? data.get("on_hold").asBoolean() : false);
            invoice.setRelease_date(getTextValue(data, "release_date"));
            invoice.setHold_comment(getTextValue(data, "hold_comment")); 
                        
            // Calculer le statut de paiement
            Double outstanding = getDoubleValue(data, "outstanding_amount");
            Double total = getDoubleValue(data, "total");
            if (outstanding != null && total != null) {
                if (outstanding == 0) {
                    invoice.setPayment_status("Paid");
                    invoice.setPaid(true);
                } else if (outstanding < total) {
                    invoice.setPayment_status("Partly Paid");
                    invoice.setPaid(false);
                } else if (data.get("on_hold").asBoolean()) {
                    invoice.setStatus("On Hold");
                    invoice.setPayment_status("On Hold");
                    invoice.setPaid(false);
                } else {
                    invoice.setPayment_status("Unpaid");
                    invoice.setPaid(false);
                }
            }
            
            // Traiter les articles
            List<InvoiceItemDTO> items = new ArrayList<>();
            if (data.has("items")) {
                for (JsonNode item : data.get("items")) {
                    InvoiceItemDTO itemDTO = new InvoiceItemDTO();
                    itemDTO.setItem_code(getTextValue(item, "item_code"));
                    itemDTO.setItem_name(getTextValue(item, "item_name"));
                    itemDTO.setQty(getDoubleValue(item, "qty"));
                    itemDTO.setUom(getTextValue(item, "uom"));
                    itemDTO.setRate(getDoubleValue(item, "rate"));
                    itemDTO.setAmount(getDoubleValue(item, "amount"));

                    items.add(itemDTO);
                }
            }
            invoice.setItems(items);
        }
        
        return invoice;
    }

    public void makePayment(String sid, String invoiceId, PaymentRequest payment) {
        // D'abord, récupérons les détails de la facture
        InvoiceDTO invoice = getInvoiceDetails(sid, invoiceId);

        // Ajouter des logs pour déboguer
        System.out.println("Status de la facture : " + invoice.getStatus());
        System.out.println("Données complètes de la facture :");
        System.out.println("ID : " + invoice.getName());
        System.out.println("Status : " + invoice.getStatus());
        System.out.println("Montant : " + invoice.getTotal());
        System.out.println("Montant restant : " + invoice.getOutstanding_amount());

        if (invoice.getOn_hold()) {
            throw new RuntimeException("Impossible de procéder au paiement : la facture est bloquée jusqu'au " + 
                invoice.getRelease_date());
        }

        if (invoice.getStatus() == null || 
            (!("Unpaid".equals(invoice.getStatus()) || "Partly Paid".equals(invoice.getStatus())))) {
            throw new RuntimeException("Impossible de procéder au paiement : facture en statut " + 
                (invoice.getStatus() != null ? invoice.getStatus() : "non défini"));
        }

        String url = baseUrl + "/api/resource/Payment Entry";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Création initiale du paiement
        Map<String, Object> data = new HashMap<>();
        data.put("doctype", "Payment Entry");
        data.put("docstatus", 1);
        data.put("payment_type", "Pay");
        data.put("party_type", "Supplier");
        data.put("party", invoice.getSupplier());
        data.put("party_name", invoice.getSupplier_name());
        data.put("company", invoice.getCompany_name());
        
        // Le compte paid_to devrait être le même que credit_to de la facture
        data.put("paid_to", invoice.getCredit_to()); 
        data.put("paid_to_account_type", invoice.getCredit_to_account_type());
        data.put("paid_to_account_currency", invoice.getCredit_to_account_currency());

        String paymentAccount = getPaymentAccount(sid, payment.getPaymentMode());
        data.put("paid_from", paymentAccount);
        data.put("paid_from_account_type", "Cash"); 
        data.put("paid_from_account_currency", invoice.getCredit_to_account_currency());
                
        // Configuration des montants
        data.put("paid_amount", payment.getAmount());
        data.put("received_amount", payment.getAmount());
        data.put("source_exchange_rate", 1.0);
        data.put("target_exchange_rate", 1.0);
        data.put("base_paid_amount", payment.getAmount());
        data.put("base_received_amount", payment.getAmount());
        
        // Informations de paiement
        data.put("posting_date", payment.getPaymentDate());
        data.put("mode_of_payment", payment.getPaymentMode());
        data.put("reference_no", payment.getReference());
        data.put("reference_date", payment.getPaymentDate());
        
        // Configuration des références de facture
        List<Map<String, Object>> references = new ArrayList<>();
        Map<String, Object> reference = new HashMap<>();
        reference.put("doctype", "Payment Entry Reference");
        reference.put("reference_doctype", "Purchase Invoice");
        reference.put("reference_name", invoiceId);
        reference.put("allocated_amount", payment.getAmount());
        reference.put("total_amount", invoice.getTotal());
        reference.put("outstanding_amount", invoice.getOutstanding_amount());
        reference.put("exchange_rate", 1.0);
        references.add(reference);
        data.put("references", references);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);
        
        try {
            ResponseEntity<JsonNode> createResponse = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                JsonNode.class
            );
            
            if (createResponse.getBody() != null && createResponse.getBody().has("data")) {
                String paymentName = createResponse.getBody().get("data").get("name").asText();
                System.err.println("Payment created : " + paymentName);
            } else {
                throw new RuntimeException("Erreur lors de la création du paiement : pas de données retournées");
            }
        } catch (Exception e) {
            System.err.println("Payment Error:");
            System.err.println(e.getMessage());
            throw new RuntimeException("Erreur lors du paiement : " + e.getMessage());
        }
    }

    private String getTextValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : null;
    }

    private Double getDoubleValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asDouble() : null;
    }

    private String getPaymentAccount(String sid, String modeOfPayment) {
        String url = baseUrl + "/api/resource/Mode of Payment/" + modeOfPayment;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
            );
            
            if (response.getBody() != null && response.getBody().has("data")) {
                JsonNode data = response.getBody().get("data");
                if (data.has("accounts")) {
                    JsonNode accounts = data.get("accounts");
                    if (accounts.isArray() && accounts.size() > 0) {
                        return accounts.get(0).get("default_account").asText();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting payment account: " + e.getMessage());
        }
        
        return "Cash - SC";  // Changer le compte par défaut à Cash - SC
    }

    //

    public Map<String, Object> getDashboardData(String sid) {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Get outgoing payments
        String paymentsUrl = baseUrl + "/api/resource/Payment Entry?fields=[\"paid_amount\",\"docstatus\"]" +
                            "&filters=[[\"payment_type\",\"=\",\"Pay\"],[\"docstatus\",\"=\",1]]";
        ResponseEntity<JsonNode> paymentsResponse = makeApiRequest(sid, paymentsUrl);
        
        // Get incoming bills
        String billsUrl = baseUrl + "/api/resource/Purchase Invoice?fields=[\"grand_total\",\"docstatus\"]" +
                         "&filters=[[\"docstatus\",\"=\",1]]";
        ResponseEntity<JsonNode> billsResponse = makeApiRequest(sid, billsUrl);
        
        // Calculate totals
        double paymentsTotal = calculateTotal(paymentsResponse.getBody(), "paid_amount");
        int paymentsCount = getCount(paymentsResponse.getBody());
        double billsTotal = calculateTotal(billsResponse.getBody(), "grand_total");
        int billsCount = getCount(billsResponse.getBody());
        
        // Build response
        Map<String, Object> outgoingPayments = new HashMap<>();
        outgoingPayments.put("total", paymentsTotal);
        outgoingPayments.put("count", paymentsCount);
        
        Map<String, Object> incomingBills = new HashMap<>();
        incomingBills.put("total", billsTotal);
        incomingBills.put("count", billsCount);
        
        dashboard.put("outgoing_payments", outgoingPayments);
        dashboard.put("incoming_bills", incomingBills);
        dashboard.put("total_amount", billsTotal - paymentsTotal);
        
        return dashboard;
    }
    
    private ResponseEntity<JsonNode> makeApiRequest(String sid, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        return restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            JsonNode.class
        );
    }
    
    private double calculateTotal(JsonNode response, String field) {
        double total = 0.0;
        if (response != null && response.has("data")) {
            JsonNode data = response.get("data");
            for (JsonNode doc : data) {
                total += getDoubleValue(doc, field);
            }
        }
        return total;
    }
    
    private int getCount(JsonNode response) {
        return response != null && response.has("data") ? response.get("data").size() : 0;
    }
}