package com.example.demo.controller;

import com.example.demo.dto.InvoiceDTO;
import com.example.demo.dto.PaymentRequest;
import com.example.demo.service.AccountingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/accounting")
public class AccountingController {
    
    private final AccountingService accountingService;

    public AccountingController(AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    @GetMapping("/invoices")
    public String listInvoices(
            @CookieValue(name = "sid", required = true) String sid,
            Model model) {
        List<InvoiceDTO> invoices = accountingService.getInvoices(sid);
        model.addAttribute("invoices", invoices);
        return "accounting/invoices";
    }

    @GetMapping("/invoices/{id}/details")
    public String showInvoiceDetails(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String id,
            Model model) {
        InvoiceDTO invoice = accountingService.getInvoiceDetails(sid, id);
        model.addAttribute("invoice", invoice);
        return "accounting/invoice-details";
    }

    @PostMapping("/invoices/{id}/pay")
    public String makePayment(
            @CookieValue(name = "sid", required = true) String sid,
            @PathVariable String id,
            @ModelAttribute PaymentRequest payment,
            RedirectAttributes redirectAttributes) {
        try {
            accountingService.makePayment(sid, id, payment);
            redirectAttributes.addFlashAttribute("success", "Paiement effectué avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du paiement: " + e.getMessage());
        }
        return "redirect:/accounting/invoices/" + id + "/details";
    }
}