package com.ecommerce.service;

import com.ecommerce.dto.response.PaymentIntentResponse;

public interface PaymentService {
    PaymentIntentResponse createPaymentIntent(String email, Long orderId);
    void handleWebhook(String payload, String sigHeader);
}
