package com.example.websocket.performance.performanceissue;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.jodah.concurrentunit.Waiter;
import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Slf4j
public class PerformanceIssueApplicationTests {

	private static final int TIMEOUT = 6;
	private static final int NUMBER_OF_CLIENTS = 2000;
	private static final int RAMPUP = 100;
	private static final int SESSION_LENGTH_IN_SECONDS = 720;
	private static final int MESSAGE_PER_MIN = 15;

	@LocalServerPort
	private int randomServerPort;

	@Autowired
	private TaskScheduler messageBrokerTaskScheduler;

	private String hostPort;

	private ScheduledThreadPoolExecutor executor;

	@Before
	public void setup() {
		hostPort = "localhost:" + randomServerPort;
		executor = new ScheduledThreadPoolExecutor(NUMBER_OF_CLIENTS * 2);
	}

	@After
	public void cleanup() {
		executor.shutdown();
	}

	@Test
	public void TestWithMultipleClients() throws Exception {

		Waiter waiter = new Waiter();

		AtomicInteger threadCounter = new AtomicInteger();

		int newThreadRateInMilliseconds = (int) (((double) RAMPUP / (double) NUMBER_OF_CLIENTS) * 1000);

		// We start one thread per user. (This is not accurate the NUMBER_OF_CLIENTS, but its amount is controlled by
		// the newThreadRateInMilliseconds combined with the waiter.await() call)
		executor.scheduleAtFixedRate(() -> {
			int threadCount = threadCounter.getAndIncrement();
			log.warn("Start Thread "+ threadCount);
			try {
				playOneClient(waiter, threadCount);
			} catch (Exception e) {
				log.error("Exception caught", e);
			}
		}, 0, newThreadRateInMilliseconds, TimeUnit.MILLISECONDS);

		waiter.await(TIMEOUT + SESSION_LENGTH_IN_SECONDS + RAMPUP, TimeUnit.SECONDS, NUMBER_OF_CLIENTS);
	}

	private void playOneClient(Waiter waiter, int threadCount)
			throws InterruptedException, ExecutionException, TimeoutException {
		// Register new client
		RestTemplate restTemplate = new RestTemplate();
		Map<String, String> response = restTemplate.getForObject("http://" + hostPort + "/register", Map.class);
		String username = response.get("username");
		String password = response.get("password");

		//Establish Websocket Connection
		WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
		stompClient.setTaskScheduler(messageBrokerTaskScheduler);
		stompClient.setDefaultHeartbeat(new long[]{0,0});
		HttpHeaders httpHeaders = createHeaders(username, password);
		WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders(httpHeaders);
		StompSession stompSession = stompClient
				.connect("ws://" + hostPort + "/play", webSocketHttpHeaders, new StompSessionHandlerAdapter() {})
				.get(TIMEOUT, TimeUnit.SECONDS);

		//Subscribe to user queue
		StompHeaders stompHeaders = new StompHeaders();
		stompHeaders.setDestination("/user/exchange/amq.direct/update");
		stompHeaders.setReceipt("r1");

		FrameHandler frameHandler = new FrameHandler();
		Subscription subscription = stompSession.subscribe(stompHeaders, frameHandler);

		CountDownLatch latch = new CountDownLatch(1);
		subscription.addReceiptTask(latch::countDown);
		latch.await(TIMEOUT, TimeUnit.SECONDS);;

		sendTestMessage(waiter, stompSession, frameHandler, Instant.now().plus(SESSION_LENGTH_IN_SECONDS, ChronoUnit.SECONDS),
			threadCount);

	}

	private void sendTestMessage(Waiter waiter, StompSession stompSession, FrameHandler frameHandler, Instant endTime,
			int threadCount) {
		executor.schedule(() -> {
			stompSession.send("/app/test", null);
			String result = null;
			try {
				result = frameHandler.readResult();
			} catch (Exception e) {
				waiter.fail(e);
			}
			waiter.assertTrue("foo".equals(result));

			// We either schedule a new execution of the test message...
			if (endTime.isAfter(Instant.now())) {
				sendTestMessage(waiter, stompSession, frameHandler, endTime, threadCount);
			} else { // ... or disconnect the client and call waiter.resume() to indicate a success.
				stompSession.disconnect();
				waiter.resume();
				log.warn("Finished Thread " + threadCount);
			}
		}, (60 / MESSAGE_PER_MIN), TimeUnit.SECONDS);
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
			String result = completableFuture.get(TIMEOUT, TimeUnit.SECONDS);
			completableFuture = new CompletableFuture<>();
			return result;
		}
	}

}
