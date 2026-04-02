package com.ecommerce.service;

import com.ecommerce.dto.request.OrderRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ecommerce.entity.Order;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;
    @InjectMocks OrderServiceImpl orderService;

    private User user;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).email("user@test.com").name("Test User").build();
        product = Product.builder().id(10L).name("Laptop")
                .price(new BigDecimal("999.99")).stockQuantity(5).build();
        CartItem cartItem = CartItem.builder().id(1L).product(product).quantity(2).build();
        cart = Cart.builder().id(1L).user(user).items(new ArrayList<>(List.of(cartItem))).build();
        cartItem.setCart(cart);
    }

    @Test
    @DisplayName("createOrder: creates order from cart and clears cart")
    void createOrder_success() {
        OrderRequest req = new OrderRequest("123 Main St, Springfield");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(1L); return o;
        });
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = orderService.createOrder("user@test.com", req);

        assertThat(resp.getTotalPrice()).isEqualByComparingTo("1999.98"); // 999.99 × 2
        assertThat(resp.getShippingAddress()).isEqualTo("123 Main St, Springfield");
        assertThat(resp.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(cart.getItems()).isEmpty();                             // cart cleared
        assertThat(product.getStockQuantity()).isEqualTo(3);              // stock deducted
    }

    @Test
    @DisplayName("createOrder: throws BadRequestException for empty cart")
    void createOrder_emptyCart() {
        cart.getItems().clear();
        OrderRequest req = new OrderRequest("123 Main St");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder("user@test.com", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    @DisplayName("createOrder: throws InsufficientStockException when stock is low")
    void createOrder_insufficientStock() {
        product.setStockQuantity(1); // only 1 in stock but cart has qty=2
        OrderRequest req = new OrderRequest("123 Main St");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder("user@test.com", req))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Laptop");
    }

    @Test
    @DisplayName("getOrder: returns order belonging to user")
    void getOrder_success() {
        Order order = Order.builder().id(1L).user(user).items(new ArrayList<>())
                .totalPrice(new BigDecimal("99.99")).shippingAddress("123 St")
                .paymentStatus(PaymentStatus.PENDING).build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(order));

        OrderResponse resp = orderService.getOrder("user@test.com", 1L);

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getPaymentStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("getOrder: throws ResourceNotFoundException when order not found or wrong user")
    void getOrder_notFound() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUser(999L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder("user@test.com", 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
