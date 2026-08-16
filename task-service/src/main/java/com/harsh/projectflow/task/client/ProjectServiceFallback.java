package com.harsh.projectflow.task.client;

import org.springframework.stereotype.Component;

@Component
public class ProjectServiceFallback implements ProjectServiceClient{

	@Override
	public boolean projectExists(Long id) {
		return false;
	}
}
