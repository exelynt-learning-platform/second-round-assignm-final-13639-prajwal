package com.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductRequest {
    @NotBlank @Size(max = 200)
    private String name;
    private String description;
    @NotNull @DecimalMin("0.01")
    private BigDecimal price;
    @NotNull @Min(0)
    private Integer stockQuantity;
    private String imageUrl;
}
