package com.example.websocket.performance.performanceissue;

import com.google.common.collect.Lists;
import java.util.Collection;
import lombok.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Value
public class User implements UserDetails {

	public static final String IS_USER = "IS_USER";

	String password;

	String username;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Lists.newArrayList(() -> IS_USER);
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
