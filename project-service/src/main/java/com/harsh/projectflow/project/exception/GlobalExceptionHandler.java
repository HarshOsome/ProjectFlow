package com.harsh.projectflow.project.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.harsh.projectflow.project.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ProjectNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleProjectNotFound(ProjectNotFoundException ex, HttpServletRequest request){
		ex.printStackTrace();
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}
	
	@ExceptionHandler(MemberAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleMemberExists(MemberAlreadyExistsException ex, HttpServletRequest request){
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}
	
	@ExceptionHandler(UnauthorizedProjectAccessException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedProjectAccessException ex, HttpServletRequest request){
		return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request){
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(fieldError -> fieldError.getDefaultMessage())
				.orElse("Validation failed");
		
		return buildResponse(HttpStatus.BAD_REQUEST , message, request);
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request){
		ex.printStackTrace(); // real cause printing.
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occured", request);
	}
	
	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request){
		ErrorResponse error = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), "project-service");
		
		return new ResponseEntity<>(error, status);
	}
	
}
