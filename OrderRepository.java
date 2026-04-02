package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    Page<Order> findByUser(User user, Pageable pageable);
    Optional<Order> findByIdAndUser(Long id, User user);
    Optional<Order> findByStripePaymentIntentId(String paymentIntentId);
}
