package com.ecommerce.service.impl;

import com.ecommerce.dto.request.OrderRequest;
import com.ecommerce.dto.response.*;
import com.ecommerce.entity.*;
import com.ecommerce.exception.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(String email, OrderRequest request) {
        User user = getUser(email);
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot create order from an empty cart");
        }

        // Validate stock & build order items
        Order order = Order.builder()
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            Product product = ci.getProduct();
            if (product.getStockQuantity() < ci.getQuantity()) {
                throw new InsufficientStockException(product.getName(), product.getStockQuantity());
            }
            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - ci.getQuantity());
            productRepository.save(product);

            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(ci.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            order.getItems().add(oi);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        order.setTotalPrice(total);
        Order saved = orderRepository.save(order);

        // Clear cart after order
        cart.getItems().clear();
        cartRepository.save(cart);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String email, Long orderId) {
        User user = getUser(email);
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String email, Pageable pageable) {
        User user = getUser(email);
        return orderRepository.findByUser(user, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    public OrderResponse toResponse(Order o) {
        List<OrderItemResponse> items = o.getItems().stream().map(i -> OrderItemResponse.builder()
                .id(i.getId())
                .productId(i.getProduct().getId())
                .productName(i.getProduct().getName())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .subtotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .build()).collect(Collectors.toList());

        return OrderResponse.builder()
                .id(o.getId())
                .userId(o.getUser().getId())
                .userEmail(o.getUser().getEmail())
                .items(items)
                .totalPrice(o.getTotalPrice())
                .shippingAddress(o.getShippingAddress())
                .paymentStatus(o.getPaymentStatus().name())
                .stripePaymentIntentId(o.getStripePaymentIntentId())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
