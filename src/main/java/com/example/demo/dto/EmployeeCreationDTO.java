package com.example.demo.dto;

import lombok.Data;

@Data
public class EmployeeCreationDTO {
    private String firstName;
    private String lastName;
    private String company;
    private String gender;
    private String dateOfJoining;
    private String dateOfBirth;
    private String personalEmail;
    private String department;
    private String designation;
} 