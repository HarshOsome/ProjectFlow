package com.harsh.projectflow.task.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harsh.projectflow.task.client.ProjectServiceClient;
import com.harsh.projectflow.task.client.UserServiceClient;
import com.harsh.projectflow.task.dto.CommentRequest;
import com.harsh.projectflow.task.dto.CommentResponse;
import com.harsh.projectflow.task.dto.TaskRequest;
import com.harsh.projectflow.task.dto.TaskResponse;
import com.harsh.projectflow.task.entity.Comment;
import com.harsh.projectflow.task.entity.Task;
import com.harsh.projectflow.task.entity.TaskStatus;
import com.harsh.projectflow.task.exception.CommentNotFoundException;
import com.harsh.projectflow.task.exception.InvalidTaskStatusTransitionException;
import com.harsh.projectflow.task.exception.TaskNotFoundException;
import com.harsh.projectflow.task.repository.CommentRepository;
import com.harsh.projectflow.task.repository.TaskRepository;

@Service

public class TaskService {

	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private ProjectServiceClient projectServiceClient;

	@Autowired
	private UserServiceClient userServiceClient;

	@Autowired
	private TaskStatusValidator taskStatusValidator;

	public TaskResponse createTask(TaskRequest request, Long createdBy) {
		boolean projectExists = projectServiceClient.projectExists(request.getProjectId());
		if (!projectExists) {
			throw new TaskNotFoundException(
					"Project not found or Project Service unavailable,project ID: " + request.getProjectId());
		}

		if (request.getAssigneeId() != null) {
			boolean assigneeExists = userServiceClient.userExists(request.getAssigneeId());
			if (!assigneeExists) {
				throw new TaskNotFoundException(
						"Assignee not found or Project Service unavailable,project ID: " + request.getAssigneeId());
			}
		}

		Task task = new Task();
		task.setTitle(request.getTitle());
		task.setDescription(request.getDescription());
		task.setStatus(TaskStatus.TODO);
		task.setPriority(request.getPriority());
		task.setProjectId(request.getProjectId());
		task.setAssigneeId(request.getAssigneeId());
		task.setCreatedBy(createdBy);
		task.setDueDate(request.getDueDate());

		Task saved = taskRepository.save(task);
		return toTaskResponse(saved);

	}

	public TaskResponse getTaskById(Long id) {
		Task task = taskRepository.findById(id)
				.orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));
		return toTaskResponse(task);
	}

	public List<TaskResponse> getTasksByProject(Long projectId) {
		return taskRepository.findByProjectId(projectId).stream().map(this::toTaskResponse).toList();
	}

	public List<TaskResponse> getTaskByAssignee(Long assigneeId) {
		return taskRepository.findByAssigneeId(assigneeId).stream().map(this::toTaskResponse).toList();
	}

	public TaskResponse updateTask(Long id, TaskRequest request) {
		Task task = taskRepository.findById(id)
				.orElseThrow(() -> new TaskNotFoundException("Task Not found with ID" + id));
		task.setTitle(request.getTitle());
		task.setDescription(request.getDescription());
		task.setPriority(request.getPriority());
		task.setAssigneeId(request.getAssigneeId());
		task.setDueDate(request.getDueDate());

		Task updated = taskRepository.save(task);
		return toTaskResponse(updated);

	}

	public void deleteTask(Long id) {
		if (!taskRepository.existsById(id)) {
			throw new TaskNotFoundException("Task Not Found with id: " + id);
		}
		taskRepository.deleteById(id);
	}

	public TaskResponse updateStatus(Long id, TaskStatus newStatus) {
		Task task = taskRepository.findById(id)
				.orElseThrow(() -> new TaskNotFoundException("Task not found with id - " + id));

		if (!taskStatusValidator.isValidTransition(task.getStatus(), newStatus)) {
			throw new InvalidTaskStatusTransitionException(
					"CANNOT TRANISITION TASK FROM " + task.getStatus() + " to " + newStatus);
		}
		task.setStatus(newStatus);
		Task updated = taskRepository.save(task);
		return toTaskResponse(updated);

	}

	public CommentResponse addComment(Long taskId, CommentRequest request, Long userId) {
		if (!taskRepository.existsById(taskId)) {
			throw new TaskNotFoundException("Task Not Found with id: " + taskId);
		}

		Comment comment = new Comment();
		comment.setTaskId(taskId);
		comment.setUserId(userId);
		comment.setContent(request.getContent());

		Comment saved = commentRepository.save(comment);
		return toCommentResponse(saved);

	}

	public List<CommentResponse> getComments(Long taskId) {
		if (!taskRepository.existsById(taskId)) {
			throw new TaskNotFoundException("Task not found with id: " + taskId);
		}

		return commentRepository.findByTaskId(taskId).stream().map(this::toCommentResponse).toList();
	}

	public void deleteComment(Long taskId, Long commentId) {
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new CommentNotFoundException("Comment not found with id" + commentId));

		if (!comment.getTaskId().equals(taskId)) {
			throw new CommentNotFoundException("Comment " + commentId + "does not belong to task " + taskId);
		}

		commentRepository.deleteById(commentId);
	}

	public boolean taskExists(Long id) {
		return taskRepository.existsById(id);
	}

	private TaskResponse toTaskResponse(Task t) {
		return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(),
				t.getProjectId(), t.getAssigneeId(), t.getCreatedBy(), t.getDueDate(), t.getCreatedAt(),
				t.getUpdatedAt());
	}

	private CommentResponse toCommentResponse(Comment c) {
		return new CommentResponse(c.getId(), c.getTaskId(), c.getUserId(), c.getContent(), c.getCreatedBy(),
				c.getUpdatedAt());
	}
}
