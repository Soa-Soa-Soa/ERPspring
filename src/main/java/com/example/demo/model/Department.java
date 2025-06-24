package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "tabDepartment")
@Data
public class Department {
    @Id
    @Column(name = "name")
    private String name;

    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "company")
    private String company;

    @Column(name = "disabled")
    private Integer disabled;
} 