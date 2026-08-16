package com.harsh.projectflow.task.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harsh.projectflow.task.dto.CommentRequest;
import com.harsh.projectflow.task.dto.CommentResponse;
import com.harsh.projectflow.task.dto.StatusUpdateRequest;
import com.harsh.projectflow.task.dto.TaskRequest;
import com.harsh.projectflow.task.dto.TaskResponse;
import com.harsh.projectflow.task.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/tasks")
public class TaskController {
	@Autowired
	private TaskService taskService;

	@PostMapping
	public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request,
			@RequestHeader("X-User-Id") Long userId) {
		TaskResponse response = taskService.createTask(request, userId);
		return new ResponseEntity<>(response, HttpStatus.CREATED);

	}

	@GetMapping("/{id}")
	public ResponseEntity<TaskResponse> getTaskId(@PathVariable Long id) {
		return ResponseEntity.ok(taskService.getTaskById(id));
	}

	@GetMapping("/project/{projectId}")
	public ResponseEntity<List<TaskResponse>> getTasksByProject(@PathVariable long projectId) {
		return ResponseEntity.ok(taskService.getTasksByProject(projectId));
	}

	@GetMapping("/assignee/{userId}")
	public ResponseEntity<List<TaskResponse>> getTasksByAssignee(@PathVariable long userId) {
		return ResponseEntity.ok(taskService.getTasksByProject(userId));
	}

	@PutMapping("/{id}")
	public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
		return ResponseEntity.ok(taskService.updateTask(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
		taskService.deleteTask(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<TaskResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request){
		return ResponseEntity.ok(taskService.updateStatus(id, request.getStatus()));
	}

	@PostMapping("/{id}/comments")
	public ResponseEntity<CommentResponse>addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request, @RequestHeader("X-User-Id") Long userId){
		CommentResponse response = taskService.addComment(id, request, userId);
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}
	
	@GetMapping("/{id}/comments")
	public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long id){
		return ResponseEntity.ok(taskService.getComments(id));
	}
	
	@DeleteMapping("/{id}/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId){
		taskService.deleteComment(id, commentId);
		return ResponseEntity.noContent().build();
	}
	
	
}
