package com.ecommerce.service;

import com.ecommerce.dto.request.OrderRequest;
import com.ecommerce.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse createOrder(String email, OrderRequest request);
    OrderResponse getOrder(String email, Long orderId);
    Page<OrderResponse> getUserOrders(String email, Pageable pageable);
    Page<OrderResponse> getAllOrders(Pageable pageable);
}
