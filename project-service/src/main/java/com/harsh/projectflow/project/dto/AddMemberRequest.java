package com.harsh.projectflow.project.dto;

import com.harsh.projectflow.project.entity.MemberRole;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberRequest {
	
	@NotNull(message = "User ID is required")
	private Long userId;
	
	@NotNull(message = "Member role is required")
	private MemberRole memberRole;

}
