package com.trading.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class MarketDataClient {

    private final RestClient restClient;

    public MarketDataClient(@Value("${market.service.url}") String marketUrl) {
        this.restClient = RestClient.builder().baseUrl(marketUrl).build();
    }

    public Map<String, Object> getStockPrice(String symbol) {
        return restClient.get()
                .uri("/api/v1/market/stocks/{symbol}", symbol)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
