package com.example.websocket.performance.performanceissue;

import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "message-queue")
@Data
public class MessageQueueProperties {

	private @NotNull String host;

	private @NotNull int port;

	private @NotNull String systemUsername;

	private @NotNull String systemPassword;

	private @NotNull String clientUsername;

	private @NotNull String clientPassword;

}
