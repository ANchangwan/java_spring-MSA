package com.example.inventory.config;

import com.example.common.dto.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class InventoryConsumer {
    @Bean
    // OrderCreatedEvent를 읽어 드리는 consumer
    Consumer<OrderCreatedEvent> processInventory(){
        return (event) ->{
            // 재고 차감 로직
            log.info("Received OrderCreatedEvent: {}", event.productId());
        };
    }
}
