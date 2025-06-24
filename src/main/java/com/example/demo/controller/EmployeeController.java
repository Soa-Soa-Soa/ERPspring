package com.example.demo.controller;

import com.example.demo.dto.EmployeeDTO;
import com.example.demo.dto.EmployeeCreationDTO;
import com.example.demo.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public String listEmployees(
            @CookieValue(name = "sid", required = true) String sid,
            Model model) {
        
        List<EmployeeDTO> employees = employeeService.getEmployees(sid);
        List<String> departments = employeeService.getDepartments(sid);
        
        model.addAttribute("employees", employees);
        model.addAttribute("departments", departments);
        
        return "employees/list";
    }

    @GetMapping("/api")
    @ResponseBody
    public List<EmployeeDTO> getEmployeesApi(
            @CookieValue(name = "sid", required = true) String sid) {
        return employeeService.getEmployees(sid);
    }

    @PostMapping("/api/updateStatus")
    @ResponseBody
    public RedirectView desactivate(
            @CookieValue(name = "sid", required = true) String sid,
            @RequestParam String employeeId,
            @RequestParam String status) {
        employeeService.updateStatus(employeeId, sid, status);

        return new RedirectView("/employees");
    }

    // Creation employee

    @GetMapping("/new")
    public String showCreateEmployeeForm(Model model, @CookieValue(name = "sid", required = true) String sid) {
        model.addAttribute("employee", new EmployeeCreationDTO());
        model.addAttribute("departments", employeeService.getDepartments(sid));
        model.addAttribute("designations", employeeService.getDesignations(sid));
        model.addAttribute("companies", employeeService.getCompanies(sid));
        
        return "employees/create-employee";
    }

    @PostMapping("/new")
    public String createEmployee(@ModelAttribute EmployeeCreationDTO employee, @CookieValue(name = "sid", required = true) String sid, RedirectAttributes redirectAttributes) {
        if (sid == null) {
            return "redirect:/login";
        }
        // Set a default company if not provided, as it's often mandatory
        if (employee.getCompany() == null || employee.getCompany().isEmpty()) {
            employee.setCompany("Frappe Technologies"); // Replace with a valid company from your ERPNext
        }
        try {
            employeeService.createEmployee(employee, sid);
            redirectAttributes.addFlashAttribute("successMessage", "Employee '" + employee.getFirstName() + "' created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create employee: " + e.getMessage());
        }
        return "redirect:/employees";
    }

    @PostMapping("/{id}/delete")
    public String deleteEmployee(@PathVariable String id, @CookieValue(name = "sid", required = true) String sid, RedirectAttributes redirectAttributes) {
        if (sid == null) {
            return "redirect:/login";
        }
        try {
            employeeService.deleteEmployee(id, sid);
            redirectAttributes.addFlashAttribute("successMessage", "Employee '" + id + "' has been deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Employee " + id + " cannot be deleted.");
        }
        return "redirect:/employees";
    }
} 