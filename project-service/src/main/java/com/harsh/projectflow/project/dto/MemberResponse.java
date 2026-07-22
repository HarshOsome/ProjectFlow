package com.harsh.projectflow.project.dto;

import java.time.LocalDateTime;

import com.harsh.projectflow.project.entity.MemberRole;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MemberResponse {
	
	private Long id;
	private Long projectId;
	private Long userId;
	private MemberRole memberRole;
	private LocalDateTime joinedAt;

}
