package com.example.common.dto;

public record OrderCreatedRequest(
        String productId,
        Integer quantity
) { }
