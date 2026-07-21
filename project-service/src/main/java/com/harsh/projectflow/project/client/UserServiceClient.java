package com.harsh.projectflow.project.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name ="user-service", url ="${user-service.url}", fallback = UserServiceFallback.class)
public interface UserServiceClient {
	
	@GetMapping("/users/{id}/exists")
	boolean userExists(@PathVariable("id") Long id);

}
