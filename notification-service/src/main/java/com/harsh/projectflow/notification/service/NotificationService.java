package com.harsh.projectflow.notification.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.harsh.projectflow.notification.dto.NotificationRequest;
import com.harsh.projectflow.notification.dto.NotificationResponse;
import com.harsh.projectflow.notification.entity.Notification;
import com.harsh.projectflow.notification.exception.EmailDeliveryException;
import com.harsh.projectflow.notification.exception.NotificationNotFoundException;
import com.harsh.projectflow.notification.repository.NotificationRepository;

@Service
public class NotificationService {

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private JavaMailSender mailSender;

	public NotificationResponse sendNotification(NotificationRequest request) {
		Notification notification = new Notification();
		notification.setUserId(request.getUserId());
		notification.setUserEmail(request.getUserEmail());
		notification.setType(request.getType());
		notification.setSubject(request.getSubject());
		notification.setMessage(request.getMessage());
		notification.setRead(false);

		Notification saved = notificationRepository.save(notification);

		try {
			SimpleMailMessage mailMessage = new SimpleMailMessage();
			mailMessage.setTo(request.getUserEmail());
			mailMessage.setSubject(request.getSubject());
			mailMessage.setText(request.getMessage());
			mailSender.send(mailMessage);
		} catch (Exception e) {
			throw new EmailDeliveryException("Notification saved but email delivery failed :" + e.getMessage());

		}
		return toNotificationResponse(saved);

	}

	public List<NotificationResponse> getUserNotifications(Long userId) {
		return notificationRepository.findByUserId(userId).stream().map(this::toNotificationResponse).toList();
	}

	public List<NotificationResponse> getUnreadNotifications(Long userId) {
		return notificationRepository.findByUserIdAndIsReadFalse(userId).stream().map(this::toNotificationResponse)
				.toList();
	}

	public NotificationResponse markAsRead(Long id) {
		Notification notification = notificationRepository.findById(id)
				.orElseThrow(() -> new NotificationNotFoundException("Notification not found with id" + id));

		notification.setRead(true);
		Notification updated = notificationRepository.save(notification);
		return toNotificationResponse(updated);
	}

	public void deleteNotification(Long id) {
		if (!notificationRepository.existsById(id)) {
			throw new NotificationNotFoundException("Notification not found with id: " + id);

		}
		notificationRepository.deleteById(id);
	}

	public NotificationResponse toNotificationResponse(Notification n) {
		return new NotificationResponse(n.getId(), n.getUserId(), n.getUserEmail(), n.getType(), n.getSubject(),
				n.getMessage(), n.isRead(), n.getCreatedAt());
	}
}
