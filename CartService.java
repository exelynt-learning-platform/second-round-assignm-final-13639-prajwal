package com.ecommerce.service;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(String email);
    CartResponse addToCart(String email, CartItemRequest request);
    CartResponse updateCartItem(String email, Long cartItemId, Integer quantity);
    CartResponse removeFromCart(String email, Long cartItemId);
    void clearCart(String email);
}
