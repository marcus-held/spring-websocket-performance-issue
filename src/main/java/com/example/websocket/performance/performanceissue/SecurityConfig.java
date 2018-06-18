package com.example.websocket.performance.performanceissue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	@RequiredArgsConstructor
	@Configuration
	@Slf4j
	public static class UserWebSecurity extends WebSecurityConfigurerAdapter {

		private final UserService userService;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.csrf().disable();

			http.httpBasic();

			http.requestMatchers()
					.antMatchers(WebsocketConfig.WEBSOCKET_HANDSHAKE_PREFIX);

			http
					.authorizeRequests()
					.anyRequest().hasAuthority(User.IS_USER);

			http
					.sessionManagement()
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		}

		@Autowired
		public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
			auth.userDetailsService(userService)
					.passwordEncoder(NoOpPasswordEncoder.getInstance());
		}

	}


}
