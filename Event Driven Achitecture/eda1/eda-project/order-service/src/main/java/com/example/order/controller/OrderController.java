package com.example.order.controller;

import com.example.common.dto.OrderCreatedRequest;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<String> createdOrder(@RequestBody OrderCreatedRequest request){
        String orderId = orderService.CreatedOrder(request);
        log.info("Created order with id {}", orderId);
        return ResponseEntity.ok("Order Created: "+orderId);
    }
}
