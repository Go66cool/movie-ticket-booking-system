package com.example.booking.service;

import com.example.booking.config.AppProperties;
import com.example.booking.domain.Role;
import com.example.booking.domain.User;
import com.example.booking.dto.AuthResponse;
import com.example.booking.dto.LoginRequest;
import com.example.booking.dto.RegisterRequest;
import com.example.booking.dto.UserDto;
import com.example.booking.exception.ConflictException;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final AppProperties props;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authManager, AppProperties props) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authManager = authManager;
        this.props = props;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("Email already registered");
        }
        User u = User.builder()
                .email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .role(Role.CUSTOMER)
                .build();
        u = userRepository.save(u);
        String token = jwtService.generate(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthResponse(token, "Bearer", props.security().jwt().ttlMinutes(), UserDto.from(u));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User u = userRepository.findByEmailIgnoreCase(req.email()).orElseThrow();
        String token = jwtService.generate(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthResponse(token, "Bearer", props.security().jwt().ttlMinutes(), UserDto.from(u));
    }
}
