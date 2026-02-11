package com.workflow.user.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.user.dto.UserSimpleResponse;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
	
	private final UserRepository userRepository;
	
	
	@GetMapping("/assigneelist")
	public List<UserSimpleResponse> list() {
	    return userRepository.findAll().stream()
	            .map(u -> new UserSimpleResponse(
	                    u.getId(),
	                    u.getName(),
	                    u.getDepartment()
	            ))
	            .toList();
	}
}
