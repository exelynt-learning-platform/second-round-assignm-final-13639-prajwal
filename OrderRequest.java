package com.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class OrderRequest {
    @NotBlank
    private String shippingAddress;
}
