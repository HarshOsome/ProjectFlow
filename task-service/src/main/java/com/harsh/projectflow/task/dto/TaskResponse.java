package com.harsh.projectflow.task.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.harsh.projectflow.task.entity.Priority;
import com.harsh.projectflow.task.entity.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskResponse {
	private Long id;
	private String title;
	private String description;
	private TaskStatus status;
	private Priority priority;
	private Long projectId;
	private Long assigneeId;
	
	private Long createdBy;  // who created this?
	
	private LocalDate duedate;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
