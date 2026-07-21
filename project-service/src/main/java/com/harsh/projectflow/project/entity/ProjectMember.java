package com.harsh.projectflow.project.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_members")
@Data
@NoArgsConstructor
@AllArgsConstructor

@EqualsAndHashCode(of = "id")

public class ProjectMember {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private Long projectId;
	
	@Column(nullable = false)
	private Long userId;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberRole memberRole;
	
	@Column(nullable = false, updatable = false)
	private LocalDateTime joinedAt;
	
	@PrePersist
	protected void onCreate() {
		this.joinedAt = LocalDateTime.now();
	}
}
