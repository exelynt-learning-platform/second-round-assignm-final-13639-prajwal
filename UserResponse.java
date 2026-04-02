package com.ecommerce.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private LocalDateTime createdAt;
}
