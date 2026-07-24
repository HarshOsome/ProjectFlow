package com.harsh.projectflow.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harsh.projectflow.project.client.UserServiceClient;
import com.harsh.projectflow.project.dto.AddMemberRequest;
import com.harsh.projectflow.project.dto.MemberResponse;
import com.harsh.projectflow.project.dto.ProjectRequest;
import com.harsh.projectflow.project.dto.ProjectResponse;
import com.harsh.projectflow.project.entity.Project;
import com.harsh.projectflow.project.entity.ProjectMember;
import com.harsh.projectflow.project.exception.MemberAlreadyExistsException;
import com.harsh.projectflow.project.exception.ProjectNotFoundException;
import com.harsh.projectflow.project.reporsitory.ProjectMemberRepository;
import com.harsh.projectflow.project.reporsitory.ProjectRepository;

import jakarta.transaction.Transactional;

@Service
public class ProjectService {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserServiceClient userServiceClient;

	// CREATE A PROJECT !!!
	
	public ProjectResponse createProject(ProjectRequest request, Long createdBy) {
		Project project = new Project();
		project.setName(request.getName());
		project.setDescription(request.getDescription());
		project.setStatus(request.getStatus());
		project.setStartDate(request.getStartDate());
		project.setDeadline(request.getDeadline());
		project.setCreatedBy(createdBy);

		Project saved = projectRepository.save(project);
		return toProjectResponse(saved);

	}

	// GET Project BY ID
	public ProjectResponse getProjectByID(long id) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ProjectNotFoundException("Project not found with id " + id));

		return toProjectResponse(project);
	}

	// GET ALL !
	public List<ProjectResponse> getAllProjects() {
		return projectRepository.findAll().stream().map(this::toProjectResponse).toList();
	}
	
	// UPDATE Project !!
	public ProjectResponse updateProject(Long id, ProjectRequest request) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ProjectNotFoundException("Project not found with id " + id));

		project.setName(request.getName());
		project.setDescription(request.getDescription());
		project.setStatus(request.getStatus());
		project.setStartDate(request.getStartDate());
		project.setDeadline(request.getDeadline());

		Project updated = projectRepository.save(project);
		return toProjectResponse(updated);
	}

	
	// IMPORTANT METHOD ! </3
	public MemberResponse addMember(Long projectId, AddMemberRequest request) {
		if (!projectRepository.existsById(projectId)) {
			throw new ProjectNotFoundException("Project not found with id " + projectId);
		}

		boolean userExists = userServiceClient.userExists(request.getUserId());
		if (!userExists) {
			throw new ProjectNotFoundException("User not found or User Service unavailable " + request.getUserId());

		}
		if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
			throw new MemberAlreadyExistsException(
					"User " + request.getUserId() + " is already a member of project" + projectId);

		}
		ProjectMember member = new ProjectMember();
		member.setProjectId(projectId);
		member.setUserId(request.getUserId());
		member.setMemberRole(request.getMemberRole());

		ProjectMember saved = projectMemberRepository.save(member);
		return toMemberResponse(saved);
	}

	public List<MemberResponse> getMembers(Long projectId) {
		if (!projectRepository.existsById(projectId)) {
			throw new ProjectNotFoundException("Project not found with id:" + projectId);
		}
		return projectMemberRepository.findByProjectId(projectId).stream().map(this::toMemberResponse).toList();
	}
	
	//DELETION------------ DXD

	public void deleteProject(Long id) {
		if (!projectRepository.existsById(id)) {
			throw new ProjectNotFoundException("Project not found with id " + id);
		}
		projectRepository.deleteById(id);
	}
	@Transactional
	public void removeMember(Long projectId, Long userId) {
		if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
			throw new ProjectNotFoundException("Member not found: userId " + userId + " on project " + projectId);
		}
		projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
	}
	
	// Only Code 
	public boolean projectExists(Long id) {
		return projectRepository.existsById(id);
	}

	
	
	
	// HELPER METHODS:
	private ProjectResponse toProjectResponse(Project p) {
		return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getStatus(), p.getStartDate(),
				p.getDeadline(), p.getCreatedBy(), p.getCreatedAt(), p.getUpdatedAt());
	}

	private MemberResponse toMemberResponse(ProjectMember m) {

		return new MemberResponse(m.getId(), m.getProjectId(), m.getUserId(), m.getMemberRole(), m.getJoinedAt());
	}

}
