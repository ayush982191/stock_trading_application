package com.trading.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Value("${user.service.url}") String userUrl) {
        this.restClient = RestClient.builder().baseUrl(userUrl).build();
    }

    public Map<String, Object> deductFunds(String userId, BigDecimal amount) {
        return restClient.post()
                .uri("/api/v1/users/{userId}/funds/deduct?amount={amount}", userId, amount)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> creditFunds(String userId, BigDecimal amount) {
        return restClient.post()
                .uri("/api/v1/users/{userId}/funds/credit?amount={amount}", userId, amount)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
