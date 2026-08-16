package com.harsh.projectflow.notification.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.harsh.projectflow.notification.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NotificationNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NotificationNotFoundException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(EmailDeliveryException.class)
	public ResponseEntity<ErrorResponse> handleEmailFailure(EmailDeliveryException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream().findFirst()
				.map(fieldError -> fieldError.getDefaultMessage()).orElse("Validation failed");
		return buildResponse(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
		ex.printStackTrace();
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "A GENERIC UNEXPECTED ERRORR OCCURED", request);

	}

	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request){
		ErrorResponse error = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), "notification-service");
		
		return new ResponseEntity<>(error,status);
	}
}
