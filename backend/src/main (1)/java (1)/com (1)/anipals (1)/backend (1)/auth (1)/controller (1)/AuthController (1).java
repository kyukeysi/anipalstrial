package com.anipals.backend.auth.controller;

import com.anipals.backend.auth.dto.AuthResponse;
import com.anipals.backend.auth.dto.LoginRequest;
import com.anipals.backend.auth.dto.RegisterRequest;
import com.anipals.backend.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
