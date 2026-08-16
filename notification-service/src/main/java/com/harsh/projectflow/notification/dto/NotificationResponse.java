package com.harsh.projectflow.notification.dto;

import java.time.LocalDateTime;

import com.harsh.projectflow.notification.entity.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationResponse {

	private Long id;
	private Long userId;
	private String userEmail;
	private NotificationType type;
	private String subject;
	private String message;
	private boolean isRead;
	private LocalDateTime createdAt;
}
