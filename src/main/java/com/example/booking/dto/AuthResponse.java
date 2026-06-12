package com.example.booking.dto;

public record AuthResponse(String token, String tokenType, long expiresInMinutes, UserDto user) {}
