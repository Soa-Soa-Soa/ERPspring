package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dto.QuotationDTO;
import com.example.demo.dto.QuotationFormDTO;
import com.example.demo.dto.QuotationItem;
import com.example.demo.dto.QuotationItemDTO;
import com.example.demo.service.ItemService;
import com.example.demo.service.QuotationService;
import com.example.demo.service.SupplierService;
import com.example.demo.service.WarehouseService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/devis")
public class QuotationController {

    @Autowired
    private QuotationService service;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private WarehouseService warehouseService;
    


    @GetMapping("/form")
    public String getSavePage(
        Model model, 
        HttpSession session, RedirectAttributes redirectAttributes, 
        @CookieValue(name = "sid", required = true) String sid){

        model.addAttribute("pageTitle", "Formulaire de Devis");
        model.addAttribute("content", "quotation-form");

        try {
            model.addAttribute("suppliers", supplierService.getSuppliers(sid));
            model.addAttribute("items", itemService.get(sid));
            model.addAttribute("warehouses", warehouseService.get(sid));
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("suppliers", List.of());
            model.addAttribute("items", List.of());
            model.addAttribute("warehouses", List.of());
        }
        return "quotation-form";
    }


    @PostMapping
    public String save(
        @ModelAttribute QuotationFormDTO form, 
        RedirectAttributes redirect,
        @CookieValue(name = "sid", required = true) String sid) {

        QuotationDTO quotation = new QuotationDTO();
        quotation.setSupplier(form.getSupplier());
        quotation.setValid_till(form.getValid_till());
        quotation.setTransaction_date(form.getDate());

        List<QuotationItemDTO> quotationItems = new ArrayList<>();
        double totalQty = 0;

        for (QuotationItem itemDto : form.getItems()) {
            QuotationItemDTO item = new QuotationItemDTO();
            item.setItem_name(itemDto.getItem());
            item.setQty(itemDto.getQty());
            item.setRate(itemDto.getRate());
            item.setWarehouse(itemDto.getWarehouse());
            
            quotationItems.add(item);
            totalQty += itemDto.getQty();
        }

        quotation.setItems(quotationItems);
        quotation.setTotal_qty(totalQty);

        try {
            service.save(sid, quotation);
            redirect.addFlashAttribute("message", "Soumission réussie");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/devis/form";
    }
}