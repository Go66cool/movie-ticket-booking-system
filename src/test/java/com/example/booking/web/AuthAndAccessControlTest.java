package com.example.booking.web;

import com.example.booking.domain.Role;
import com.example.booking.domain.User;
import com.example.booking.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndAccessControlTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    @Transactional
    void seed() {
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .email("admin@test.local")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Admin").role(Role.ADMIN).build());
        userRepository.save(User.builder()
                .email("cust@test.local")
                .passwordHash(passwordEncoder.encode("password1"))
                .fullName("Cust").role(Role.CUSTOMER).build());
    }

    @Test
    void registerNewCustomer_returnsToken() throws Exception {
        String body = """
                {"email":"new@test.local","password":"password1","fullName":"New User"}
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void loginAsCustomer_thenAccessAdminEndpoint_returns403() throws Exception {
        String token = login("cust@test.local", "password1");
        String body = """
                {"name":"Forbidden City","state":"NA"}
                """;
        mockMvc.perform(post("/api/admin/cities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateCity() throws Exception {
        String token = login("admin@test.local", "admin123");
        String body = """
                {"name":"NewCity","state":"NS"}
                """;
        mockMvc.perform(post("/api/admin/cities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewCity"));
    }

    @Test
    void unauthenticatedRequestToProtectedEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/bookings/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidLogin_returns401() throws Exception {
        String body = """
                {"email":"admin@test.local","password":"wrong"}
                """;
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerValidationFailure_returns400() throws Exception {
        String body = """
                {"email":"not-an-email","password":"x","fullName":""}
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    private String login(String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
        String res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(res);
        String token = node.get("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
