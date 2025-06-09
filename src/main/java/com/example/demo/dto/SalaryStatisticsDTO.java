package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.Map;

public class SalaryStatisticsDTO {
    private int year;
    private int month;
    private String monthName;
    private BigDecimal totalGrossPay;
    private BigDecimal totalDeductions;
    private BigDecimal totalNetPay;
    private Map<String, BigDecimal> earningsComponents;
    private Map<String, BigDecimal> deductionsComponents;
    private int employeeCount;

    // Getters and Setters
    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public BigDecimal getTotalGrossPay() {
        return totalGrossPay;
    }

    public void setTotalGrossPay(BigDecimal totalGrossPay) {
        this.totalGrossPay = totalGrossPay;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public void setTotalDeductions(BigDecimal totalDeductions) {
        this.totalDeductions = totalDeductions;
    }

    public BigDecimal getTotalNetPay() {
        return totalNetPay;
    }

    public void setTotalNetPay(BigDecimal totalNetPay) {
        this.totalNetPay = totalNetPay;
    }

    public Map<String, BigDecimal> getEarningsComponents() {
        return earningsComponents;
    }

    public void setEarningsComponents(Map<String, BigDecimal> earningsComponents) {
        this.earningsComponents = earningsComponents;
    }

    public Map<String, BigDecimal> getDeductionsComponents() {
        return deductionsComponents;
    }

    public void setDeductionsComponents(Map<String, BigDecimal> deductionsComponents) {
        this.deductionsComponents = deductionsComponents;
    }

    public int getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(int employeeCount) {
        this.employeeCount = employeeCount;
    }
} 