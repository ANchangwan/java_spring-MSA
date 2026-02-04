package com.example.delivery.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimitConfig {

    @Bean
    KeyResolver userKeyResolver() {
        return exchange ->{
            String userId = exchange
                    .getRequest()
                    .getHeaders()
                    .getFirst("X-User-ID");

            if(userId == null) {
                userId = Objects.requireNonNull(exchange
                        .getRequest()
                        .getRemoteAddress())
                        .getAddress()
                        .getHostAddress();
            }
            return Mono.just(userId);
        };
    }
}
