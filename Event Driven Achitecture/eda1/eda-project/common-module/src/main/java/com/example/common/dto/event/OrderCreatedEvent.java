package com.example.common.dto.event;

import java.time.LocalDateTime;

public record OrderCreatedEvent(
        String orderId,
        String productId,
        Integer quantity,
        LocalDateTime createTime
) { }
