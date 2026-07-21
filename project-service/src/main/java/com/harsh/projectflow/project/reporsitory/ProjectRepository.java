package com.harsh.projectflow.project.reporsitory;

import org.springframework.data.jpa.repository.JpaRepository;

import com.harsh.projectflow.project.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
	

}
