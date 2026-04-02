package com.ecommerce.service.impl;

import com.ecommerce.dto.response.PaymentIntentResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    @Transactional
    public PaymentIntentResponse createPaymentIntent(String email, Long orderId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order " + orderId + " is already paid");
        }

        try {
            // Convert total to cents (Stripe uses smallest currency unit)
            long amountInCents = order.getTotalPrice()
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .putMetadata("orderId", orderId.toString())
                    .putMetadata("userEmail", email)
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // Save intent id to order for webhook reconciliation
            order.setStripePaymentIntentId(intent.getId());
            order.setPaymentStatus(PaymentStatus.PROCESSING);
            orderRepository.save(order);

            return PaymentIntentResponse.builder()
                    .clientSecret(intent.getClientSecret())
                    .paymentIntentId(intent.getId())
                    .orderId(orderId)
                    .amountInCents(amountInCents)
                    .currency("usd")
                    .build();

        } catch (StripeException e) {
            log.error("Stripe error creating PaymentIntent for order {}: {}", orderId, e.getMessage());
            throw new BadRequestException("Payment processing error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new BadRequestException("Invalid webhook signature");
        }

        log.info("Received Stripe event: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                handlePaymentSuccess(intent);
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                handlePaymentFailed(intent);
            }
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handlePaymentSuccess(PaymentIntent intent) {
        orderRepository.findByStripePaymentIntentId(intent.getId()).ifPresent(order -> {
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);
            log.info("Order {} marked as PAID via Stripe intent {}", order.getId(), intent.getId());
        });
    }

    private void handlePaymentFailed(PaymentIntent intent) {
        orderRepository.findByStripePaymentIntentId(intent.getId()).ifPresent(order -> {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            log.warn("Order {} payment FAILED via Stripe intent {}", order.getId(), intent.getId());
        });
    }
}
