package com.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {
    @NotBlank @Size(min = 2, max = 60)
    private String name;
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 6, max = 40)
    private String password;
}
