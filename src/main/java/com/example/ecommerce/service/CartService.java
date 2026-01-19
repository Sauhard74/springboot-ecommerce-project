package com.example.ecommerce.service;

import com.example.ecommerce.dto.AddToCartRequest;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {
    
    private final CartRepository cartRepository;
    private final ProductService productService;
    
    @Transactional
    public CartItem addToCart(AddToCartRequest request) {
        // Validate product exists
        Product product = productService.getProductById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));
        
        // Check stock availability
        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getStock());
        }
        
        // Check if item already in cart
        Optional<CartItem> existingItem = cartRepository.findByUserIdAndProductId(
                request.getUserId(), request.getProductId());
        
        if (existingItem.isPresent()) {
            // Update quantity
            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            return cartRepository.save(cartItem);
        } else {
            // Add new item
            CartItem cartItem = new CartItem();
            cartItem.setId(UUID.randomUUID().toString());
            cartItem.setUserId(request.getUserId());
            cartItem.setProductId(request.getProductId());
            cartItem.setQuantity(request.getQuantity());
            return cartRepository.save(cartItem);
        }
    }
    
    public List<CartItem> getUserCart(String userId) {
        return cartRepository.findByUserId(userId);
    }
    
    @Transactional
    public void clearCart(String userId) {
        cartRepository.deleteByUserId(userId);
    }
    
    public List<Map<String, Object>> getCartWithProducts(String userId) {
        List<CartItem> cartItems = cartRepository.findByUserId(userId);
        return cartItems.stream()
                .map(item -> {
                    Product product = productService.getProductById(item.getProductId())
                            .orElse(null);
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", item.getId());
                    itemMap.put("productId", item.getProductId());
                    itemMap.put("quantity", item.getQuantity());
                    if (product != null) {
                        Map<String, Object> productMap = new HashMap<>();
                        productMap.put("id", product.getId());
                        productMap.put("name", product.getName());
                        productMap.put("price", product.getPrice());
                        itemMap.put("product", productMap);
                    }
                    return itemMap;
                })
                .toList();
    }
}
