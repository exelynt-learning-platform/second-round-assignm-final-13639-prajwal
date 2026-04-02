package com.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CartItemRequest {
    @NotNull
    private Long productId;
    @NotNull @Min(1)
    private Integer quantity;
}
