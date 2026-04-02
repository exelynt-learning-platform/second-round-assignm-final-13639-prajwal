package com.ecommerce.controller;

import com.ecommerce.dto.response.PaymentIntentResponse;
import com.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/create-intent/{orderId}
     * Creates a Stripe PaymentIntent for the given order.
     * Returns a clientSecret which the frontend uses to confirm payment.
     */
    @PostMapping("/create-intent/{orderId}")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.createPaymentIntent(user.getUsername(), orderId));
    }

    /**
     * POST /api/payments/webhook
     * Stripe webhook endpoint — receives payment_intent.succeeded / failed events.
     * This endpoint must be publicly accessible (no JWT required).
     * Register this URL in your Stripe Dashboard.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok("Webhook received");
    }
}
