package com.example.delivery.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Global Filter - Request Path: {}", exchange.getRequest().getPath());
        log.info("Global Filter - Request ID: {}", exchange.getRequest().getId());
        // 요청을 다음 필터(또는 서비스)로 진행
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    // Post Filter: 응답 처리 후 실행
                    log.info("Global Filter - Response Status: {}", exchange.getResponse().getStatusCode());
                }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
