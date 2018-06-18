package com.example.websocket.performance.performanceissue;

import com.google.common.collect.Maps;
import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

	private Map<String, User> users = Maps.newConcurrentMap();

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return users.get(username);
	}

	public void registerUser(User user) {
		users.put(user.getUsername(), user);
	}
}
