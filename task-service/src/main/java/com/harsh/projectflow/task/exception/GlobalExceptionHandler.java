package com.harsh.projectflow.task.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.harsh.projectflow.task.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(TaskNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex, HttpServletRequest request){
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}
	@ExceptionHandler(CommentNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException ex, HttpServletRequest request){
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}
	@ExceptionHandler(InvalidTaskStatusTransitionException.class)
	public ResponseEntity<ErrorResponse> handleInvalidTaskStatusTransition(InvalidTaskStatusTransitionException ex, HttpServletRequest request){
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request){
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(fieldError -> fieldError.getDefaultMessage())
				.orElse("Validation failed");
		
		return buildResponse(HttpStatus.BAD_REQUEST, message, request);
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request){
		ex.printStackTrace();
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An Unexpected error occured", request);
	}
	
	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request){
		ErrorResponse error = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), "task-service");
		return new ResponseEntity<>(error, status);
	}
	
	
	
	
}
