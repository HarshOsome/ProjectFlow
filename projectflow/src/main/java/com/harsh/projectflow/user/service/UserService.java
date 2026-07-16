package com.harsh.projectflow.user.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.harsh.projectflow.user.dto.AuthResponse;
import com.harsh.projectflow.user.dto.LoginRequest;
import com.harsh.projectflow.user.dto.RegisterRequest;
import com.harsh.projectflow.user.dto.UserResponse;
import com.harsh.projectflow.user.entity.User;
import com.harsh.projectflow.user.exception.EmailAlreadyExistsException;
import com.harsh.projectflow.user.exception.InvalidCredentialException;
import com.harsh.projectflow.user.exception.UserNotFoundException;
import com.harsh.projectflow.user.repository.UserRepository;
import com.harsh.projectflow.user.security.JwtUtil;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtil jwtUtil;

	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new EmailAlreadyExistsException("Email Already registered" + request.getEmail());
		}
		User user = new User();
		user.setName(request.getName());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setRole(request.getRole());
		user.setActive(true);

		User savedUser = userRepository.save(user);

		String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

		return new AuthResponse(token, "Bearer", savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

	}

	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new InvalidCredentialException("Invalid Email or password"));
		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new InvalidCredentialException("Invalid email or password");
		}

		String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
		return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(), user.getRole().name());
	}

	public UserResponse getUserById(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException("USER not found with id" + id));

		return toUserResponse(user);
	}

	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream().map(this::toUserResponse).toList();
	}

	public boolean userExists(Long id) {
		return userRepository.existsById(id);
	}

	private UserResponse toUserResponse(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive(),
				user.getCreatedAt());

	}
}
