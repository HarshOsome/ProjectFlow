package com.harsh.projectflow.task.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.harsh.projectflow.task.entity.TaskStatus;

@Component
public class TaskStatusValidator {
	private final Map<TaskStatus, Set<TaskStatus>> allowedTransitions = new EnumMap<>(TaskStatus.class);
	
	public TaskStatusValidator() {
		allowedTransitions.put(TaskStatus.TODO, EnumSet.of(TaskStatus.IN_PROGRESS,TaskStatus.CANCELLED));
		
		allowedTransitions.put(TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.IN_REVIEW,TaskStatus.CANCELLED));
		
		allowedTransitions.put(TaskStatus.IN_REVIEW, EnumSet.of(TaskStatus.DONE,TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED));
		
		allowedTransitions.put(TaskStatus.DONE, EnumSet.noneOf(TaskStatus.class));
		
		allowedTransitions.put(TaskStatus.CANCELLED, EnumSet.noneOf(TaskStatus.class));
	}
	public boolean isValidTransition(TaskStatus current, TaskStatus next) {
		return allowedTransitions.get(current).contains(next);
	}
}
