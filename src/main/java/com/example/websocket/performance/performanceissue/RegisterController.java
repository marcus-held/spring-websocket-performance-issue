package com.example.websocket.performance.performanceissue;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RegisterController {

	private final UserService userService;

	private AtomicLong userGenerator = new AtomicLong(0);

	@GetMapping("register")
	public Map<String, String> register() {

		String username = Long.toString(userGenerator.getAndIncrement());
		String password = username;

		userService.registerUser(new User(username, password));

		return ImmutableMap.of(
				"username", username,
				"password", password
		);
	}

}
