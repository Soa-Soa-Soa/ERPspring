package com.example.demo.config;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseCookie.ResponseCookieBuilder;
import java.time.Duration;

public class CookieUtil {
    public static ResponseCookie createCookie(String name, String value) {
        ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, value)
            .path("/")
            .secure(true)
            .httpOnly(true)
            .sameSite("Lax")
            .maxAge(Duration.ofDays(3));
        
        return cookieBuilder.build();
    }
}