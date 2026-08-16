package com.harsh.projectflow.notification.dto;

import com.harsh.projectflow.notification.entity.NotificationType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {

	@NotNull(message = "User ID is required")
	private Long userId;
	
	@NotBlank(message = "User mail is required")
	@Email(message = "Email must be valid")
	private String userEmail;
	
	@NotNull(message = "Notification Type is required")
	private NotificationType type;
	
	@NotBlank(message = "Subject is required")
	private String subject;
	
	@NotBlank(message = "Message is required")
	private String message;
}
