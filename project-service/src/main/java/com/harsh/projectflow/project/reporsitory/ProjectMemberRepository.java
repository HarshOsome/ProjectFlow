package com.harsh.projectflow.project.reporsitory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.harsh.projectflow.project.entity.ProjectMember;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
	
	List<ProjectMember> findByProjectId(Long projectId);

	Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);
	
	boolean existsByProjectIdAndUserId(Long projectId, Long userId);
	
	void deleteByProjectIdAndUserId(Long projectId, Long userId);

}
