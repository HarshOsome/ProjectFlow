package com.harsh.projectflow.task.dto;

import java.time.LocalDate;

import com.harsh.projectflow.task.entity.Priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskRequest {
	
	@NotBlank(message = "Title is required")
	private String title;
	
	private String description;
	
	@NotNull(message = "Priority is required")
	private Priority priority;
	
	@NotNull(message = "Project ID is required")
	private Long projectId;
	
	private Long assigneeId;
	
	private LocalDate dueDate;

}
