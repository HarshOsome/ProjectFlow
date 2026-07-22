package com.harsh.projectflow.project.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.harsh.projectflow.project.entity.ProjectStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectResponse {
	private Long id;
	private String name;
	private String description;
	private ProjectStatus status;
	private LocalDate startDate;
	private LocalDate deadline;
	private Long createdBy;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
