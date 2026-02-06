package com.example.order.service;

import com.example.common.dto.OrderCreatedRequest;
import com.example.common.dto.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    // 메시지 발행 할 때 사용
    private final StreamBridge streamBridge;

    @Transactional
    public String CreatedOrder(OrderCreatedRequest request) {
        log.info("Creating order {}", request);
        String orderId = UUID.randomUUID().toString();

        log.info("Created order {}", orderId);

        // dto 생성
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                request.productId(),
                request.quantity(),
                LocalDateTime.now()
        );

        streamBridge.send("order-created-out-0",event);
        log.info("Created order {}", orderId);
        return orderId;
    }
}
