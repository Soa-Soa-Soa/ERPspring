package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {
    
    @Value("${erpnext.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;

    public AuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public LoginResponse login(LoginRequest request){
        String loginUrl = baseUrl + "/api/method/login";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<LoginResponse> response = restTemplate.exchange(
            loginUrl,
            HttpMethod.POST,
            entity,
            LoginResponse.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            LoginResponse loginResponse = response.getBody();
            
            // Extraire les cookies
            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies != null) {
                for (String cookie : cookies) {
                    if (cookie.contains("sid=")) {
                        loginResponse.setSid(extractCookieValue(cookie, "sid"));
                    } else if (cookie.contains("user_id=")) {
                        loginResponse.setUserId(extractCookieValue(cookie, "user_id"));
                    } else if (cookie.contains("user_lang=")) {
                        loginResponse.setUserLang(extractCookieValue(cookie, "user_lang"));
                    } else if (cookie.contains("system_user=")) {
                        loginResponse.setSystemUser(extractCookieValue(cookie, "system_user"));
                    }
                }
            }
            
            System.out.println("Session Cookies:");
            System.out.println("SID: " + loginResponse.getSid());
            System.out.println("User ID: " + loginResponse.getUserId());
            System.out.println("User Lang: " + loginResponse.getUserLang());
            System.out.println("System User: " + loginResponse.getSystemUser());
            
            return loginResponse;
        } else {
            throw new RuntimeException("Authentication failed");
        }
    }

    private String extractCookieValue(String cookie, String key) {
        String[] parts = cookie.split(";");
        for (String part : parts) {
            if (part.trim().startsWith(key + "=")) {
                return part.trim().substring(key.length() + 1);
            }
        }
        return null;
    }

    public boolean isValidSession(String sid) {
        if (sid == null) return false;

        String validateUrl = baseUrl + "/api/method/frappe.auth.get_logged_user";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "sid=" + sid);
        
        try {
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                validateUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, String>>() {}
            );
            
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}