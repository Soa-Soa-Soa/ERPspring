package com.example.demo.controller;

import com.example.demo.dto.EmployeeDTO;
import com.example.demo.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
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
} 