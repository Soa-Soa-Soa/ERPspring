package com.example.demo.service;

import com.example.demo.dto.SalaryDetailDTO;
import com.example.demo.dto.SalarySlipDTO;
import com.example.demo.dto.SalaryStatisticsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalaryStatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(SalaryStatisticsService.class);

    @Autowired
    private SalaryService salaryService;

    public List<SalaryStatisticsDTO> getYearlyStatistics(int year, String sid) {
        List<SalaryStatisticsDTO> yearlyStats = new ArrayList<>();
        
        // Pour chaque mois de l'année
        for (Month month : Month.values()) {
            // Récupérer tous les bulletins de salaire du mois
            List<SalarySlipDTO> monthlySlips = salaryService.getMonthlySalarySlips(sid, YearMonth.of(year, month));
            
            if (!monthlySlips.isEmpty()) {
                logger.info("Processing month: {} - Found {} salary slips", month, monthlySlips.size());
                
                SalaryStatisticsDTO monthStats = new SalaryStatisticsDTO();
                monthStats.setYear(year);
                monthStats.setMonth(month.getValue());
                monthStats.setMonthName(month.toString());
                monthStats.setEmployeeCount(monthlySlips.size());

                // Calculer les totaux
                BigDecimal totalGross = monthlySlips.stream()
                    .map(slip -> BigDecimal.valueOf(slip.getGrossPay()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                BigDecimal totalDeductions = monthlySlips.stream()
                    .map(slip -> BigDecimal.valueOf(slip.getTotalDeduction()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                BigDecimal totalNet = monthlySlips.stream()
                    .map(slip -> BigDecimal.valueOf(slip.getNetPay()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                monthStats.setTotalGrossPay(totalGross);
                monthStats.setTotalDeductions(totalDeductions);
                monthStats.setTotalNetPay(totalNet);

                // Calculer les totaux par composant de salaire
                Map<String, BigDecimal> earningsComponents = new HashMap<>();
                Map<String, BigDecimal> deductionsComponents = new HashMap<>();
                
                // Traiter chaque bulletin de salaire
                for (SalarySlipDTO slip : monthlySlips) {
                    logger.info("Processing salary slip for employee: {}", slip.getEmployeeName());
                    
                    // Traiter les gains
                    if (slip.getEarnings() != null) {
                        for (SalaryDetailDTO earning : slip.getEarnings()) {
                            String componentName = earning.getComponent();
                            BigDecimal amount = BigDecimal.valueOf(earning.getAmount());
                            earningsComponents.merge(componentName, amount, BigDecimal::add);
                            logger.info("Added earning: {} = {}", componentName, amount);
                        }
                    }
                    
                    // Traiter les déductions
                    if (slip.getDeductions() != null) {
                        for (SalaryDetailDTO deduction : slip.getDeductions()) {
                            String componentName = deduction.getComponent();
                            BigDecimal amount = BigDecimal.valueOf(deduction.getAmount());
                            deductionsComponents.merge(componentName, amount, BigDecimal::add);
                            logger.info("Added deduction: {} = {}", componentName, amount);
                        }
                    }
                }

                logger.info("Final earnings components: {}", earningsComponents);
                logger.info("Final deductions components: {}", deductionsComponents);

                monthStats.setEarningsComponents(earningsComponents);
                monthStats.setDeductionsComponents(deductionsComponents);
                yearlyStats.add(monthStats);
            }
        }

        // Trier par mois
        return yearlyStats.stream()
            .sorted(Comparator.comparingInt(SalaryStatisticsDTO::getMonth))
            .collect(Collectors.toList());
    }
} 