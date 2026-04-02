package com.ecommerce.service;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.impl.ProductServiceImpl;
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

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @InjectMocks ProductServiceImpl productService;

    private Product sampleProduct() {
        return Product.builder().id(1L).name("Widget").description("A fine widget")
                .price(new BigDecimal("19.99")).stockQuantity(100).imageUrl("img.jpg").build();
    }

    @Test
    @DisplayName("createProduct: persists and returns response")
    void createProduct_success() {
        ProductRequest req = ProductRequest.builder().name("Widget").description("A fine widget")
                .price(new BigDecimal("19.99")).stockQuantity(100).imageUrl("img.jpg").build();
        when(productRepository.save(any())).thenReturn(sampleProduct());

        ProductResponse resp = productService.createProduct(req);

        assertThat(resp.getName()).isEqualTo("Widget");
        assertThat(resp.getPrice()).isEqualByComparingTo("19.99");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("getProductById: returns product when found")
    void getProductById_found() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct()));
        ProductResponse resp = productService.getProductById(1L);
        assertThat(resp.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getProductById: throws ResourceNotFoundException when missing")
    void getProductById_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateProduct: updates fields and returns response")
    void updateProduct_success() {
        Product existing = sampleProduct();
        ProductRequest req = ProductRequest.builder().name("Updated").description("New desc")
                .price(new BigDecimal("29.99")).stockQuantity(50).imageUrl("new.jpg").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse resp = productService.updateProduct(1L, req);

        assertThat(resp.getName()).isEqualTo("Updated");
        assertThat(resp.getPrice()).isEqualByComparingTo("29.99");
        assertThat(resp.getStockQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("deleteProduct: calls deleteById when product exists")
    void deleteProduct_success() {
        when(productRepository.existsById(1L)).thenReturn(true);
        productService.deleteProduct(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteProduct: throws when product not found")
    void deleteProduct_notFound() {
        when(productRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAllProducts: returns paginated list")
    void getAllProducts_paginated() {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct()));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<ProductResponse> result = productService.getAllProducts(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Widget");
    }
}
