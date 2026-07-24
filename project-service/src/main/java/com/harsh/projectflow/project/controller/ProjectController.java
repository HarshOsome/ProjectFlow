package com.harsh.projectflow.project.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harsh.projectflow.project.dto.AddMemberRequest;
import com.harsh.projectflow.project.dto.MemberResponse;
import com.harsh.projectflow.project.dto.ProjectRequest;
import com.harsh.projectflow.project.dto.ProjectResponse;
import com.harsh.projectflow.project.service.ProjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/projects")
public class ProjectController {
	@Autowired
	private ProjectService projectService;
	
	@PostMapping
	public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request, @RequestHeader("X-User-Id") Long UserId){
		ProjectResponse response = projectService.createProject(request, UserId);
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}
	
	@GetMapping
	public ResponseEntity<List<ProjectResponse>> getAllProjects(){
		return ResponseEntity.ok(projectService.getAllProjects());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<ProjectResponse> getProjectById(@PathVariable long id){
		return ResponseEntity.ok(projectService.getProjectByID(id));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<ProjectResponse> updateProject(@PathVariable long id, @Valid @RequestBody ProjectRequest request){
		return ResponseEntity.ok(projectService.updateProject(id, request));
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteProject(@PathVariable long id){
		projectService.deleteProject(id);
		return ResponseEntity.noContent().build();
		
	}
	
	@PostMapping("/{id}/members")
	public ResponseEntity<MemberResponse> addMember(@PathVariable Long id, @Valid @RequestBody AddMemberRequest request){
		MemberResponse response = projectService.addMember(id, request);
		
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}
	
	@GetMapping("/{id}/members")
	public ResponseEntity<List<MemberResponse>>getMembers(@PathVariable Long id){
		return ResponseEntity.ok(projectService.getMembers(id));
	}
	
	
	@DeleteMapping("/{id}/members/{userId}")
	public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId){
		projectService.removeMember(id, userId);
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping("/{id}/exists")
	public ResponseEntity<Boolean> projectExists(@PathVariable Long id){
		return ResponseEntity.ok(projectService.projectExists(id));
	}

}
