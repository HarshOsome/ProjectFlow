package com.harsh.projectflow.project.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
	private LocalDateTime timestamp;
	private int String;
	private String error;
	private String message;
	private String path;
	private String service;

}
