package com.example.demo.controller;

import com.example.demo.config.CookieUtil;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.service.AccountingService;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    private final AuthService authService;
    private final AccountingService accountingService;

    public LoginController(AuthService authService, AccountingService accountingService) {
        this.accountingService = accountingService;
        this.authService = authService;
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(LoginRequest loginRequest, Model model, HttpServletResponse response,  HttpSession session) {
        try {
            LoginResponse loginResponse = authService.login(loginRequest);
            
            if (loginResponse.getSid() != null) {
                response.addHeader(HttpHeaders.SET_COOKIE, 
                    CookieUtil.createCookie("sid", loginResponse.getSid()).toString());
            }
            if (loginResponse.getUserId() != null) {
                response.addHeader(HttpHeaders.SET_COOKIE, 
                    CookieUtil.createCookie("user_id", loginResponse.getUserId()).toString());
            }
            if (loginResponse.getUserLang() != null) {
                response.addHeader(HttpHeaders.SET_COOKIE, 
                    CookieUtil.createCookie("user_lang", loginResponse.getUserLang()).toString());
            }
            if (loginResponse.getSystemUser() != null) {
                response.addHeader(HttpHeaders.SET_COOKIE, 
                    CookieUtil.createCookie("system_user", loginResponse.getSystemUser()).toString());
            }

            session.setAttribute("loginResponse", loginResponse);
            session.setAttribute("username", loginRequest.getUsr());

            model.addAttribute("loginResponse", loginResponse);
            model.addAttribute("username", loginRequest.getUsr());
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid credentials");
            return "login";
        }
    }

    @GetMapping("/erp/dashboard")
    public String showDashboard(HttpSession session, Model model, @CookieValue("sid") String sid) {
        model.addAttribute("loginResponse", session.getAttribute("loginResponse"));
        model.addAttribute("username", session.getAttribute("username"));

        Map<String, Object> data = accountingService.getDashboardData(sid);
        model.addAttribute("data", data);

        return "erp/dashboard";
    }

    @GetMapping("/dashboard")
    public String showIndex(HttpSession session, Model model, @CookieValue("sid") String sid) {
        model.addAttribute("loginResponse", session.getAttribute("loginResponse"));
        model.addAttribute("username", session.getAttribute("username"));

        return "index";
    }

    @GetMapping("/check-auth")
    @ResponseBody
    public ResponseEntity<Boolean> checkAuth(@CookieValue(name = "sid", required = false) String sid) {
        return ResponseEntity.ok(sid != null);
    }

    @GetMapping("/status")
    public String showStatus(Model model, HttpSession session) {
        LoginResponse loginResponse = (LoginResponse) session.getAttribute("loginResponse");
        String username = (String) session.getAttribute("username");
        
        if (loginResponse == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("loginResponse", loginResponse);
        model.addAttribute("username", username);
        return "status";
    }
}