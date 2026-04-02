package com.ecommerce.dto.response;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentIntentResponse {
    private String clientSecret;
    private String paymentIntentId;
    private Long orderId;
    private Long amountInCents;
    private String currency;
}
