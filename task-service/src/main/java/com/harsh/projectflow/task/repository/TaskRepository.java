package com.harsh.projectflow.task.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.harsh.projectflow.task.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
	
	List<Task> findByProjectId(Long projectId);
	
	List<Task> findByAssigneeId(Long assigneeId); 
}
