package com.example.booking.dto;

import com.example.booking.domain.User;

public record UserDto(Long id, String email, String fullName, String role) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name());
    }
}
