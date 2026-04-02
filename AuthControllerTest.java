package com.ecommerce.controller;

import com.ecommerce.dto.request.*;
import com.ecommerce.dto.response.AuthResponse;
import com.ecommerce.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;

    // Security beans needed by WebMvcTest
    @MockBean com.ecommerce.security.JwtUtils jwtUtils;
    @MockBean com.ecommerce.security.JwtAuthFilter jwtAuthFilter;
    @MockBean org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @MockBean org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Test
    @DisplayName("POST /api/auth/register → 201 with token")
    void register_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "alice@test.com", "secret123");
        AuthResponse resp = AuthResponse.builder().token("jwt").email("alice@test.com")
                .id(1L).name("Alice").role("ROLE_USER").build();
        when(authService.register(any())).thenReturn(resp);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register → 400 when email is blank")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "not-an-email", "secret123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login → 200 with token")
    void login_returns200() throws Exception {
        LoginRequest req = new LoginRequest("alice@test.com", "secret123");
        AuthResponse resp = AuthResponse.builder().token("jwt").email("alice@test.com")
                .id(1L).name("Alice").role("ROLE_USER").build();
        when(authService.login(any())).thenReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"));
    }
}
