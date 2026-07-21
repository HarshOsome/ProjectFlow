package com.harsh.projectflow.user.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.harsh.projectflow.user.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice

public class GlobalExceptionHandler {

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {

		ErrorResponse error = new ErrorResponse(LocalDateTime.now(), 
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(), 
				ex.getMessage(), 
				request.getRequestURI(),
				"user-service");

		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);

	}

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex, HttpServletRequest request) {

		ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(), ex.getMessage(), request.getRequestURI(), "user-service");

		return new ResponseEntity<>(error, HttpStatus.CONFLICT);

	}

	@ExceptionHandler(InvalidCredentialException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialException ex,
			HttpServletRequest request) {

		ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(), ex.getMessage(), request.getRequestURI(), "user-service");

		return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);

	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		String message = ex.getBindingResult().getFieldErrors().stream().findFirst()
				.map(fieldError -> fieldError.getDefaultMessage()).orElse("Validation failed");

		ErrorResponse error = new ErrorResponse(LocalDateTime.now(), 
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				message,
				request.getRequestURI(),
				"user-service");

		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);

	}
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request){
		
		ErrorResponse error = new ErrorResponse(
				LocalDateTime.now(), 
				HttpStatus.INTERNAL_SERVER_ERROR.value(), 
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), 
				"An Unexpected error Occured",
				request.getRequestURI(), 
				"user-service"
				);
		
		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
		
	}

}
