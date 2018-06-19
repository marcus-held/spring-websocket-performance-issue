package com.example.websocket.performance.performanceissue;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.jodah.concurrentunit.Waiter;
import org.apache.tomcat.util.codec.binary.Base64;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Slf4j
public class PerformanceIssueApplicationTests {

	@LocalServerPort
	private int randomServerPort;

	private String hostPort;

	@Before
	public void setup() {
		hostPort = "localhost:" + randomServerPort;

	}

	/**
	 * This test only works when it runs before the test with multiple clients, when rabbitMQ is used as a
	 * message broker.
	 */
	@Test
	public void TestSingleClient() throws Exception {

		Waiter waiter = new Waiter();

		int numberOfClients = 1;

		// We start one thread for every virtual client
		for (int i = 0; i < numberOfClients; i++) {
			new Thread(() -> {
				try {
					playOneClient(waiter);
				} catch (Exception e) {
					waiter.fail(e);
				}
			}).run();
		}

		waiter.await(10, TimeUnit.SECONDS, numberOfClients);
	}

	@Test
	public void TestWithMultipleClients() throws Exception {

		Waiter waiter = new Waiter();

		int numberOfClients = 10;

		// We start one thread for every virtual client
		for (int i = 0; i < numberOfClients; i++) {
			new Thread(() -> {
				try {
					playOneClient(waiter);
				} catch (Exception e) {
					waiter.fail(e);
				}
			}).run();
		}

		waiter.await(10, TimeUnit.SECONDS, numberOfClients);
	}

	private void playOneClient(Waiter waiter)
			throws InterruptedException, ExecutionException, TimeoutException {
		// Register new client
		RestTemplate restTemplate = new RestTemplate();
		Map<String, String> response = restTemplate.getForObject("http://" + hostPort + "/register", Map.class);
		String username = response.get("username");
		String password = response.get("password");

		//Establish Websocket Connection
		WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
		HttpHeaders httpHeaders = createHeaders(username, password);
		WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders(httpHeaders);
		StompSession stompSession = stompClient
				.connect("ws://" + hostPort + "/play", webSocketHttpHeaders, new StompSessionHandlerAdapter() {
				})
				.get(1, TimeUnit.SECONDS);

		//Subscribe to user queue
		FrameHandler frameHandler = new FrameHandler();
		stompSession.subscribe("/user/exchange/amq.direct/update", frameHandler);

		String subscribeResult = frameHandler.readResult();
		waiter.assertTrue(Boolean.valueOf(subscribeResult));

		//Send test request
		stompSession.send("/app/test", null);

		String result = frameHandler.readResult();
		waiter.assertTrue("foo".equals(result));
		waiter.resume();
	}

	// For authentication
	HttpHeaders createHeaders(String username, String password) {
		return new HttpHeaders() {{
			String auth = username + ":" + password;
			byte[] encodedAuth = Base64.encodeBase64(
					auth.getBytes(Charset.forName("US-ASCII")));
			String authHeader = "Basic " + new String(encodedAuth);
			set("Authorization", authHeader);
		}};
	}

	/**
	 * Simple frame handler that populates it completableFuture by the received payload.
	 */
	class FrameHandler implements StompFrameHandler {

		@Getter
		private CompletableFuture<String> completableFuture = new CompletableFuture<>();

		@Override
		public Type getPayloadType(StompHeaders headers) {
			return Object.class;
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			completableFuture.complete(new String((byte[]) payload));
		}

		/**
		 * Reads the result of the completableFuture and resets it in order to receive the next frame.
		 */
		String readResult() throws InterruptedException, ExecutionException, TimeoutException {
			String result = completableFuture.get(6, TimeUnit.SECONDS);
			completableFuture = new CompletableFuture<>();
			return result;
		}
	}

}
