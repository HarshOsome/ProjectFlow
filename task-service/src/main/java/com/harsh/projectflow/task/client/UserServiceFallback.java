package com.harsh.projectflow.task.client;

import org.springframework.stereotype.Component;

@Component
public class UserServiceFallback implements UserServiceClient {

	@Override
	public boolean userExists(Long id) {
		// TODO Auto-generated method stub
		return false;
	}
	

}
