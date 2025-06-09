package com.example.demo.controller;

import com.example.demo.service.EmployeeService;
import com.example.demo.service.SalaryService;
import com.example.demo.service.SalaryStatisticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/hrms")
public class HrmsDashboardController {

    private final EmployeeService employeeService;
    private final SalaryService salaryService;
    private final SalaryStatisticsService statisticsService;

    public HrmsDashboardController(
            EmployeeService employeeService,
            SalaryService salaryService,
            SalaryStatisticsService statisticsService) {
        this.employeeService = employeeService;
        this.salaryService = salaryService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(
            @CookieValue(name = "sid", required = true) String sid,
            Model model) {
        
        Map<String, Object> dashboardData = new HashMap<>();
        
        // Données des employés
        Map<String, Object> employeeStats = new HashMap<>();
        var employees = employeeService.getEmployees(sid);
        employeeStats.put("total", employees.size());
        employeeStats.put("active", employees.stream()
                .filter(e -> "Active".equals(e.getStatus()))
                .count());
        dashboardData.put("employees", employeeStats);
        
        // Données des salaires du mois en cours
        Map<String, Object> salaryStats = new HashMap<>();
        var currentMonth = YearMonth.now();
        var salarySlips = salaryService.getMonthlySalarySlips(sid, currentMonth);
        salaryStats.put("total", salarySlips.stream()
                .mapToDouble(slip -> slip.getGrossPay())
                .sum());
        salaryStats.put("count", salarySlips.size());
        dashboardData.put("salaries", salaryStats);
        
        // Statistiques pour les graphiques
        var statistics = statisticsService.getYearlyStatistics(currentMonth.getYear(), sid);
        
        model.addAttribute("data", dashboardData);
        model.addAttribute("statistics", statistics);
        
        return "hrms/dashboard";
    }
} 