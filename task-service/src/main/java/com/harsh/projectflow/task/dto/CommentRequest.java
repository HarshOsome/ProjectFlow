package com.harsh.projectflow.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {
	@NotBlank(message = "Content is required")
	private String content;
}
