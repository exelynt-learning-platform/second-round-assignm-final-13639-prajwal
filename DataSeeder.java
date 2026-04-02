package com.ecommerce.config;

import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedProducts();
        log.info("✅ Demo data seeded successfully");
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;

        User admin = User.builder()
                .name("Admin User")
                .email("admin@ecommerce.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ROLE_ADMIN)
                .build();
        userRepository.save(admin);
        cartRepository.save(Cart.builder().user(admin).build());

        User user = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(user);
        cartRepository.save(Cart.builder().user(user).build());

        log.info("Seeded users: admin@ecommerce.com / admin123  |  john@example.com / password123");
    }

    private void seedProducts() {
        if (productRepository.count() > 0) return;

        productRepository.save(Product.builder().name("Wireless Headphones")
                .description("Premium noise-cancelling over-ear headphones with 30h battery life.")
                .price(new BigDecimal("89.99")).stockQuantity(50)
                .imageUrl("https://example.com/images/headphones.jpg").build());

        productRepository.save(Product.builder().name("Mechanical Keyboard")
                .description("Compact TKL mechanical keyboard with Cherry MX Red switches and RGB backlight.")
                .price(new BigDecimal("129.99")).stockQuantity(30)
                .imageUrl("https://example.com/images/keyboard.jpg").build());

        productRepository.save(Product.builder().name("USB-C Hub 7-in-1")
                .description("Multi-port hub: 4K HDMI, 3×USB-A, SD/microSD, 100W PD charging.")
                .price(new BigDecimal("39.99")).stockQuantity(100)
                .imageUrl("https://example.com/images/hub.jpg").build());

        productRepository.save(Product.builder().name("27\" 4K Monitor")
                .description("IPS panel, 144Hz, HDR400, wide colour gamut for design and gaming.")
                .price(new BigDecimal("499.99")).stockQuantity(15)
                .imageUrl("https://example.com/images/monitor.jpg").build());

        productRepository.save(Product.builder().name("Ergonomic Office Chair")
                .description("Lumbar support, adjustable armrests, mesh back, 5-year warranty.")
                .price(new BigDecimal("299.99")).stockQuantity(20)
                .imageUrl("https://example.com/images/chair.jpg").build());
    }
}
