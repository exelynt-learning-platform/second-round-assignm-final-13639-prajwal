package com.ecommerce.service;

import com.ecommerce.dto.request.*;
import com.ecommerce.dto.response.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
