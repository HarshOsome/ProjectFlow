package com.harsh.projectflow.task.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "project-service", url = "${project-service.url}", fallback = ProjectServiceFallback.class)
public interface ProjectServiceClient {
	
	@GetMapping("/projects/{id}/exists")
	boolean projectExists(@PathVariable("id") Long id);

}
