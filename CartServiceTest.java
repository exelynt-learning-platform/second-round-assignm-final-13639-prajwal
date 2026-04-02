package com.ecommerce.service;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.impl.CartServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @InjectMocks CartServiceImpl cartService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;

    @BeforeEach
    void setup() {
        testUser = User.builder().id(1L).email("user@test.com").name("Test").build();
        testCart = Cart.builder().id(1L).user(testUser).items(new ArrayList<>()).build();
        testProduct = Product.builder().id(10L).name("Gadget")
                .price(new BigDecimal("25.00")).stockQuantity(20).build();
    }

    @Test
    @DisplayName("getCart: returns empty cart for user")
    void getCart_empty() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        CartResponse resp = cartService.getCart("user@test.com");

        assertThat(resp.getItems()).isEmpty();
        assertThat(resp.getTotalPrice()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("addToCart: adds new item correctly")
    void addToCart_newItem() {
        CartItemRequest req = new CartItemRequest(10L, 2);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse resp = cartService.addToCart("user@test.com", req);

        assertThat(resp.getItems()).hasSize(1);
        assertThat(resp.getTotalPrice()).isEqualByComparingTo("50.00");
        assertThat(resp.getTotalItems()).isEqualTo(2);
    }

    @Test
    @DisplayName("addToCart: throws InsufficientStockException when stock is low")
    void addToCart_insufficientStock() {
        testProduct.setStockQuantity(1);
        CartItemRequest req = new CartItemRequest(10L, 5);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> cartService.addToCart("user@test.com", req))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Gadget");
    }

    @Test
    @DisplayName("removeFromCart: removes item owned by user")
    void removeFromCart_success() {
        CartItem item = CartItem.builder().id(5L).cart(testCart).product(testProduct).quantity(1).build();
        testCart.getItems().add(item);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse resp = cartService.removeFromCart("user@test.com", 5L);

        assertThat(resp.getItems()).isEmpty();
        verify(cartItemRepository).delete(item);
    }

    @Test
    @DisplayName("removeFromCart: throws when item does not belong to user's cart")
    void removeFromCart_wrongItem() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));

        assertThatThrownBy(() -> cartService.removeFromCart("user@test.com", 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("clearCart: empties all items")
    void clearCart_success() {
        CartItem item = CartItem.builder().id(5L).cart(testCart).product(testProduct).quantity(2).build();
        testCart.getItems().add(item);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(testUser)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cartService.clearCart("user@test.com");

        assertThat(testCart.getItems()).isEmpty();
    }
}
