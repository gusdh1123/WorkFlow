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
	
	private final UserRepository userRepository;  // User 관련 DB 접근

	// 업무 담당자 선택용 리스트 조회
	@GetMapping("/assigneelist")
	public List<UserSimpleResponse> list() {
	    // DB에서 모든 사용자와 소속 부서 정보 조회
	    return userRepository.findAllWithDepartment()
	            .stream()
	            // DTO로 변환: id, 이름, 부서명만 반환
	            .map(u -> new UserSimpleResponse(
	                u.getId(),
	                u.getName(),
	                u.getDepartment().getName()
	            ))
	            .toList();
	}
}
