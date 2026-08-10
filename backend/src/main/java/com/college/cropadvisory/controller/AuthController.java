package com.college.cropadvisory.controller;

import com.college.cropadvisory.dto.AuthResponse;
import com.college.cropadvisory.dto.LoginRequest;
import com.college.cropadvisory.dto.SignupRequest;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        User user = userService.registerUser(request);
        String token = userService.authenticateUser(
                new LoginRequest(request.getEmail(), request.getPassword()));
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.authenticateUser(request);
        User user = userService.getUserByEmail(request.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getRole().name()));
    }
}