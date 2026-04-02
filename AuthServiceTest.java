package com.ecommerce.service;

import com.ecommerce.dto.request.*;
import com.ecommerce.dto.response.AuthResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.repository.*;
import com.ecommerce.security.JwtUtils;
import com.ecommerce.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock CartRepository cartRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authManager;
    @Mock JwtUtils jwtUtils;

    @InjectMocks AuthServiceImpl authService;

    @Test
    @DisplayName("register: success creates user and returns token")
    void register_success() {
        RegisterRequest req = new RegisterRequest("Alice", "alice@test.com", "secret123");
        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0); u.setId(1L); return u;
        });
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtils.generateTokenFromEmail(req.getEmail())).thenReturn("jwt-token");

        AuthResponse resp = authService.register(req);

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getEmail()).isEqualTo("alice@test.com");
        assertThat(resp.getRole()).isEqualTo("ROLE_USER");
        verify(userRepository).save(any(User.class));
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("register: throws BadRequestException when email already exists")
    void register_duplicateEmail() {
        RegisterRequest req = new RegisterRequest("Bob", "existing@test.com", "pass");
        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login: success returns token")
    void login_success() {
        LoginRequest req = new LoginRequest("alice@test.com", "secret123");
        User user = User.builder().id(1L).name("Alice").email(req.getEmail())
                .password("hashed").role(Role.ROLE_USER).build();
        Authentication auth = mock(Authentication.class);
        when(authManager.authenticate(any())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(user);
        when(jwtUtils.generateToken(auth)).thenReturn("login-token");

        AuthResponse resp = authService.login(req);

        assertThat(resp.getToken()).isEqualTo("login-token");
        assertThat(resp.getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("login: bad credentials throws exception")
    void login_badCredentials() {
        LoginRequest req = new LoginRequest("x@test.com", "wrong");
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
