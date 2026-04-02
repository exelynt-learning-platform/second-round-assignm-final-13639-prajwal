package com.ecommerce.service.impl;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.*;
import com.ecommerce.entity.*;
import com.ecommerce.exception.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        Cart cart = getCartByEmail(email);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(String email, CartItemRequest request) {
        Cart cart = getCartByEmail(email);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(product.getName(), product.getStockQuantity());
        }

        CartItem existing = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);
        if (existing != null) {
            int newQty = existing.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQty) {
                throw new InsufficientStockException(product.getName(), product.getStockQuantity());
            }
            existing.setQuantity(newQty);
            cartItemRepository.save(existing);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(String email, Long cartItemId, Integer quantity) {
        Cart cart = getCartByEmail(email);
        CartItem item = findCartItem(cart, cartItemId);

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            if (item.getProduct().getStockQuantity() < quantity) {
                throw new InsufficientStockException(item.getProduct().getName(), item.getProduct().getStockQuantity());
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(String email, Long cartItemId) {
        Cart cart = getCartByEmail(email);
        CartItem item = findCartItem(cart, cartItemId);
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void clearCart(String email) {
        Cart cart = getCartByEmail(email);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getCartByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + email));
    }

    private CartItem findCartItem(Cart cart, Long cartItemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem not found or does not belong to your cart"));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(item -> CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .imageUrl(item.getProduct().getImageUrl())
                .unitPrice(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build()).collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .totalPrice(total)
                .totalItems(items.stream().mapToInt(CartItemResponse::getQuantity).sum())
                .build();
    }
}
