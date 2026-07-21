package com.harsh.projectflow.project.client;

import org.springframework.stereotype.Component;

@Component
public class UserServiceFallback implements UserServiceClient{
	
	@Override
	public boolean userExists(Long id) {
		return false;
	}
}
