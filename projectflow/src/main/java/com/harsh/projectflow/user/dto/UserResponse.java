package com.harsh.projectflow.user.dto;

import java.time.LocalDateTime;

import com.harsh.projectflow.user.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
//@NoArgsConstructor
public class UserResponse {

	private Long id;
	private String name;
	private String email;
	private Role role;
	
	private boolean isActive;
	private LocalDateTime createdAt;
}
