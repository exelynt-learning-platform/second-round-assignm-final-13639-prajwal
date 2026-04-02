package com.ecommerce.controller;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** GET /api/cart */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(cartService.getCart(user.getUsername()));
    }

    /** POST /api/cart/items */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addToCart(user.getUsername(), request));
    }

    /** PUT /api/cart/items/{cartItemId}?quantity=3 */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItem(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long cartItemId,
            @RequestParam @Min(0) Integer quantity) {
        return ResponseEntity.ok(cartService.updateCartItem(user.getUsername(), cartItemId, quantity));
    }

    /** DELETE /api/cart/items/{cartItemId} */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long cartItemId) {
        return ResponseEntity.ok(cartService.removeFromCart(user.getUsername(), cartItemId));
    }

    /** DELETE /api/cart */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserDetails user) {
        cartService.clearCart(user.getUsername());
        return ResponseEntity.noContent().build();
    }
}
