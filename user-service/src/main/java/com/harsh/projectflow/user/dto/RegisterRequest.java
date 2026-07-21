package com.harsh.projectflow.user.dto;



import com.harsh.projectflow.user.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
	
	@NotBlank(message = "Name is required")
	private String name;
	
	@NotBlank(message = "Email is Required")
	@Email(message = "Email must be valid")
	private String email;
	
	@NotBlank(message = "Password")
	@Size(min = 6, message = "Password must be at least 6 characters")
	private String password;
	
	@NotNull(message = "Role is Required")
	private Role role;
	
	
	
}
