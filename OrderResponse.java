package com.ecommerce.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private List<OrderItemResponse> items;
    private BigDecimal totalPrice;
    private String shippingAddress;
    private String paymentStatus;
    private String stripePaymentIntentId;
    private LocalDateTime createdAt;
}
