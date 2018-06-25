package com.example.websocket.performance.performanceissue;

import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebsocketController {

	private static final String USER_UPDATE_SUBSCRIBE_PATH = "/exchange/amq.direct/update";

	private final SimpMessagingTemplate template;

	@MessageMapping("test")
	public void test(Principal principal) {
		template.convertAndSendToUser(principal.getName(), USER_UPDATE_SUBSCRIBE_PATH, "foo");
	}

}
