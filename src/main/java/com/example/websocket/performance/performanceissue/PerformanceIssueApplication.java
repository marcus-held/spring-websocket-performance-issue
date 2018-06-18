package com.example.websocket.performance.performanceissue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties( {
		MessageQueueProperties.class
})
public class PerformanceIssueApplication {

	public static void main(String[] args) {
		SpringApplication.run(PerformanceIssueApplication.class, args);
	}

}
