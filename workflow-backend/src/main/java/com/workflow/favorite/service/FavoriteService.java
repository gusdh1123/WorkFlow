package com.workflow.favorite.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.favorite.entity.FavoriteEntity;
import com.workflow.favorite.repository.FavoriteRepository;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.repasitory.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.Role;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

	private final FavoriteRepository favoriteRepository;
	private final UserRepository userRepository;
	private final TaskRepository taskRepository;

    // 즐겨찾기 등록, 해제 (토글)
	public boolean toggleFavorite(Long userId, Long taskId) {
		
	    if (userId == null)
	        throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");

	    // Task 조회
	    TaskEntity task = taskRepository.findById(taskId)
	            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다. id=" + taskId));

	    if (task.isDeleted()) {
	        throw new ApiException(ErrorCode.BAD_REQUEST, "삭제된 업무는 즐겨찾기 할 수 없습니다.");
	    }

	    // 로그인 유저 조회
	    UserEntity loginUser = userRepository.findById(userId)
	            .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));

	    // 권한 체크
	    Role role = loginUser.getRole();
	    boolean canAccess = role == Role.ADMIN
	            || task.getCreatedBy().getId().equals(userId)
	            || (task.getAssignee() != null && task.getAssignee().getId().equals(userId))
	            || (role == Role.MANAGER && loginUser.getDepartment().getId().equals(task.getWorkDepartment().getId()));

	    if (!canAccess) {
	        throw new ApiException(ErrorCode.UNAUTHORIZED, "즐겨찾기 권한이 없습니다.");
	    }

	    // 기존 즐겨찾기 여부 확인
	    Optional<FavoriteEntity> existing = favoriteRepository.findByUserAndTask(loginUser, task);

	    if (existing.isPresent()) {
	        // 즐겨찾기 되어 있으면 삭제
	        favoriteRepository.delete(existing.get());
	        return false; // 이제 즐겨찾기 아님
	    } else {
	        // 즐겨찾기 등록
	        FavoriteEntity fav = FavoriteEntity.builder()
	                .user(loginUser)
	                .task(task)
	                .build();
	        favoriteRepository.save(fav);
	        return true; // 이제 즐겨찾기 됨
	    }
	}


}