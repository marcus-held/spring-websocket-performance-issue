package com.example.websocket.performance.performanceissue;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {

	public static final String WEBSOCKET_HANDSHAKE_PREFIX = "/play";
	private static final String[] WEBSOCKET_SUBSCRIBE_PATH = {"/topic/", "/exchange/"};
	private static final String[] WEBSOCKET_DESTINATION_PREFIX = {"/app", "/user"};
	private final MessageQueueProperties properties;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker(WEBSOCKET_SUBSCRIBE_PATH);

		config.setApplicationDestinationPrefixes(WEBSOCKET_DESTINATION_PREFIX);
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint(WEBSOCKET_HANDSHAKE_PREFIX);
	}


}
