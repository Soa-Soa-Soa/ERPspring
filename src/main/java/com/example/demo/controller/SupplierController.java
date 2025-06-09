package com.example.demo.controller;

import com.example.demo.dto.PurchaseOrderDTO;
import com.example.demo.dto.QuotationDTO;
import com.example.demo.dto.SupplierDTO;
import com.example.demo.dto.UpdatePriceRequest;
import com.example.demo.service.PurchaseOrderService;
import com.example.demo.service.QuotationService;
import com.example.demo.service.SupplierService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {
    
    private final SupplierService supplierService;
    private final QuotationService quotationService;
    private final PurchaseOrderService purchaseOrderService;

    public SupplierController(SupplierService supplierService, QuotationService quotationService, PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
        this.quotationService = quotationService;
        this.supplierService = supplierService;
    }


    @GetMapping
    public String listSuppliers(
            @CookieValue(name = "sid", required = true) String sid,
            Model model) {
        List<SupplierDTO> suppliers = supplierService.getSuppliers(sid);
        model.addAttribute("suppliers", suppliers);
        return "suppliers/list";
    }

    @GetMapping("/{supplierId}/quotations")
    public String listSupplierQuotations(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String supplierId,
            Model model) {
        List<QuotationDTO> quotations = quotationService.getSupplierQuotations(sid, supplierId);
        model.addAttribute("quotations", quotations);
        model.addAttribute("supplierId", supplierId);
        return "suppliers/quotations";
    }

    @GetMapping("/{supplierId}/quotations/{quotationId}/details")
    public String showQuotationDetails(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String supplierId,
            @PathVariable String quotationId,
            Model model) {
        QuotationDTO quotation = quotationService.getQuotationDetails(sid, quotationId);
        model.addAttribute("quotation", quotation);
        model.addAttribute("supplierId", supplierId);
        return "suppliers/quotation-details";
    }

    @PostMapping("/{supplierId}/quotations/{quotationId}/submit")
    public String submitQuotation(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String supplierId,
            @PathVariable String quotationId,
            RedirectAttributes redirectAttributes) {
        try {
            quotationService.submitQuotation(sid, quotationId);
            redirectAttributes.addFlashAttribute("success", "Devis soumis avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la soumission du devis: " + e.getMessage());
        }
        return "redirect:/suppliers/" + supplierId + "/quotations/" + quotationId + "/details";
    }

    @PostMapping("/{supplierId}/quotations/{quotationId}/cancel")
    public String cancelQuotation(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String supplierId,
            @PathVariable String quotationId,
            RedirectAttributes redirectAttributes) {
        try {
            quotationService.cancelQuotation(sid, quotationId);
            redirectAttributes.addFlashAttribute("success", "Devis annulé avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'annulation du devis: " + e.getMessage());
        }
        return "redirect:/suppliers/" + supplierId + "/quotations/" + quotationId + "/details";
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<SupplierDTO>> getSuppliers(
            @CookieValue(name = "sid", required = true) String sid) {
        return ResponseEntity.ok(supplierService.getSuppliers(sid));
    }

    @PostMapping("/{supplierId}/quotations/{quotationId}/updatePrice")
    public String updateItemPrice(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String supplierId,
            @PathVariable String quotationId,
            @RequestParam String itemName,
            @RequestParam String itemCode,
            @RequestParam Double newRate,
            RedirectAttributes redirectAttributes) {
        try {
            UpdatePriceRequest request = new UpdatePriceRequest();
            request.setItemName(itemName);
            request.setItemCode(itemCode);
            request.setNewRate(newRate);
            
            quotationService.updateItemPrice(sid, quotationId, request);
            redirectAttributes.addFlashAttribute("success", "Prix mis à jour avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour du prix: " + e.getMessage());
        }
        
        return "redirect:/suppliers/" + supplierId + "/quotations/" + quotationId + "/details";
    }

    @GetMapping("/{supplierId}/orders")
    public String listSupplierOrders(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String supplierId,
            Model model) {
        List<PurchaseOrderDTO> orders = purchaseOrderService.getSupplierOrders(sid, supplierId);
        model.addAttribute("orders", orders);
        model.addAttribute("supplierId", supplierId);
        return "suppliers/orders";
    }

    @GetMapping("/{supplierId}/orders/{orderId}/details")
    public String showOrderDetails(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String supplierId,
            @PathVariable String orderId,
            Model model) {
        PurchaseOrderDTO order = purchaseOrderService.getOrderDetails(sid, orderId);
        model.addAttribute("order", order);
        model.addAttribute("supplierId", supplierId);
        return "suppliers/order-details";
    }

    @GetMapping("/quotation/create")
    public String getForm(){
        return "createquotation";
    }
}