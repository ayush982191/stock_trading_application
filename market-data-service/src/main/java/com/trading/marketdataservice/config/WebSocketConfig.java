package com.trading.marketdataservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * client connect to web-socket endpoint.
 * client subscribes to /topic/prices/{symbol}
 * client receives updates in real time. no polling needed.
 * this is exactly what real time trading app works.
 *
 *
 */

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry){
        // topics prefixed with /topic - broadcast to all subscriber
        registry.enableSimpleBroker("/topic");
        // application destination prefix.
        // client messages to /app/** routed to mapping @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");


    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        // websocket endpoint - client connect here.
        // socjs fallback for browser that don't support websocket.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

    }



}
