package com.example.demo.config;

import com.example.demo.service.AnomalyAlertWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class AnomalyWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AnomalyAlertWebSocketHandler anomalyAlertWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(anomalyAlertWebSocketHandler, "/ws/anomaly-alert")
                .setAllowedOriginPatterns("*");
    }
}
