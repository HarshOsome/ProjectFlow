package com.harsh.projectflow.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.harsh.projectflow.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long>{
	
	List<Notification> findByUserId(Long userId);
	
	List<Notification> findByUserIdAndIsReadFalse(Long userId);

}
