package com.ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CartResponse {
    private Long id;
    private List<CartItemResponse> items;
    private BigDecimal totalPrice;
    private int totalItems;
}
