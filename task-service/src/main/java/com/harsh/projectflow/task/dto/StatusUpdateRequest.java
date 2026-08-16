package com.harsh.projectflow.task.dto;

import com.harsh.projectflow.task.entity.TaskStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {
	@NotNull(message = "Status is required")
	private TaskStatus status;
}
