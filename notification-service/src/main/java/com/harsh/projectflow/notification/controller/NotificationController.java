package com.harsh.projectflow.notification.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harsh.projectflow.notification.dto.NotificationRequest;
import com.harsh.projectflow.notification.dto.NotificationResponse;
import com.harsh.projectflow.notification.service.NotificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
	
	@Autowired
	private NotificationService notificationService;
	
	@PostMapping("/send")
	public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody NotificationRequest request){
		NotificationResponse response = notificationService.sendNotification(request);
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}
	
	@GetMapping("/users/{userId}")
	public ResponseEntity<List<NotificationResponse>> getUserNotification(@PathVariable Long userId){
		return ResponseEntity.ok(notificationService.getUserNotifications(userId));
	}
	
	@GetMapping("user/{userId}/unread")
	public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(@PathVariable Long userId){
		return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
	}
	
	
	@PatchMapping("/{id}/read")
	public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id){
		return ResponseEntity.ok(notificationService.markAsRead(id));
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteNotification(@PathVariable Long id){
		notificationService.deleteNotification(id);
		return ResponseEntity.noContent().build();
	}
}
