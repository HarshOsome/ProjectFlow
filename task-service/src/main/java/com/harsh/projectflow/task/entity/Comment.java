package com.harsh.projectflow.task.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Comment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private Long taskId;
	
	@Column(nullable = false)
	private Long userId;
	
	@Column(nullable =false, length = 2000)
	private String content;
	
	@Column(nullable =false, updatable =false)
	private LocalDateTime createdBy;
	
	@Column(nullable = false)
		private LocalDateTime updatedAt;
	
	
	@PrePersist
	protected void OnCreate() {
		this.createdBy = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}
	
	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

}
