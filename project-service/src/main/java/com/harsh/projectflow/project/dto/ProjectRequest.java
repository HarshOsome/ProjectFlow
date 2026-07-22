package com.harsh.projectflow.project.dto;

import java.time.LocalDate;

import com.harsh.projectflow.project.entity.ProjectStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectRequest {
	@NotBlank(message = "Project name is required")
	private String name;
	
	private String description;
	
	@NotNull(message = "Status is required")
	private ProjectStatus status;
	
	private LocalDate startDate;
	
	private LocalDate deadline;
}
