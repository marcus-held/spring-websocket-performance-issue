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
		config.enableStompBrokerRelay(WEBSOCKET_SUBSCRIBE_PATH)
				.setAutoStartup(true)
				.setRelayHost(properties.getHost())
				.setRelayPort(properties.getPort())
				.setSystemHeartbeatReceiveInterval(10000)
				.setSystemHeartbeatSendInterval(10000)
				.setSystemLogin(properties.getSystemUsername())
				.setSystemPasscode(properties.getSystemPassword())
				.setClientLogin(properties.getClientUsername())
				.setClientPasscode(properties.getClientPassword())
				.setUserRegistryBroadcast("/topic/vil-user-registry")
				.setUserDestinationBroadcast("/topic/vil-unresolved-user-destination");

		config.setApplicationDestinationPrefixes(WEBSOCKET_DESTINATION_PREFIX);
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint(WEBSOCKET_HANDSHAKE_PREFIX);
	}


}
