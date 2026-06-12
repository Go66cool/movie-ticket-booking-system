package com.example.booking.web;

import com.example.booking.dto.AuthResponse;
import com.example.booking.dto.LoginRequest;
import com.example.booking.dto.RegisterRequest;
import com.example.booking.dto.UserDto;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.AuthService;
import com.example.booking.service.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        Long id = SecurityUtil.currentUserId();
        return ResponseEntity.ok(UserDto.from(userRepository.findById(id).orElseThrow()));
    }
}
