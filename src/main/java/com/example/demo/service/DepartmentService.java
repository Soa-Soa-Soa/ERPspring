package com.example.demo.service;

import com.example.demo.dto.DepartmentDTO;
import com.example.demo.model.Department;
import com.example.demo.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private DepartmentDTO convertToDto(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setName(department.getName());
        dto.setDepartmentName(department.getDepartmentName());
        dto.setCompany(department.getCompany());
        dto.setDisabled(department.getDisabled());
        return dto;
    }
} 