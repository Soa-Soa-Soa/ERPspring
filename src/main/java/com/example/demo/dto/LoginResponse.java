package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LoginResponse {
    private String message;
    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("home_page")
    private String homePage;
    private String sid;
    private String userId;
    private String userLang;
    private String systemUser;
    private String sessionExpiry;
}