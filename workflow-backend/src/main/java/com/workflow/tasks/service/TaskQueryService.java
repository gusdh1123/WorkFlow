package com.workflow.tasks.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.attachment.dto.AttachmentResponse;
import com.workflow.attachment.service.AttachmentService;
import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.favorite.entity.FavoriteEntity;
import com.workflow.favorite.repository.FavoriteRepository;
import com.workflow.tasks.dto.TaskResponse;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.enums.TaskVisibility;
import com.workflow.tasks.repasitory.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.Role;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskQueryService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final AttachmentService attachmentService;

    // 업무 목록 조회
    @Transactional(readOnly = true)
    public Page<TaskResponse> list(String scope, TaskStatus status, Long userId, Long deptId, int page, int size, String sort) {

        // 로그인 사용자 확인
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 기본 scope 설정
        if (scope == null || scope.isBlank()) scope = "all";
        
        // 삭제된 업무 탭이면 기본 정렬을 deletedAtDesc로
        if ("deleted".equals(scope) && (sort == null || sort.isBlank())) {
            // 삭제 탭은 기본값으로 최근순
            sort = "deletedAtDesc";
        } 

        // priorityDesc, priorityAsc 여부 확인
        boolean usePrioritySort = "priorityDesc".equals(sort) || "priorityAsc".equals(sort);

        
        // Pageable 객체 생성
        
        // "priorityDesc" 정렬일 경우, DB 쿼리에서 우선순위 정렬을 처리하기 때문에
        // Pageable에는 Sort를 따로 지정하지 않고 페이지 번호와 사이즈만 설정
        // 그 외의 sort 조건일 경우, Sort 객체를 생성해 PageRequest에 적용
        Pageable pageable;
        if (usePrioritySort) {
            // 우선순위 정렬
            Sort prioritySort = "priorityAsc".equals(sort)
                    ? Sort.by(Sort.Order.asc("priority"), Sort.Order.asc("id")) // 낮은 순
                    : Sort.by(Sort.Order.desc("priority"), Sort.Order.asc("id")); // 높은 순

            pageable = PageRequest.of(
                    Math.max(page, 0),
                    Math.min(Math.max(size, 1), 9),
                    prioritySort
            );
        } else {
            Sort sortObj = switch (sort) {
                case "createdAtDesc" -> Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                );
                case "createdAtAsc" -> Sort.by(
                        Sort.Order.asc("createdAt"),
                        Sort.Order.asc("id")
                );
                case "dueDateAsc" -> Sort.by(
                        Sort.Order.asc("dueDate"),
                        Sort.Order.desc("id")
                );
                case "dueDateDesc" -> Sort.by(
                        Sort.Order.desc("dueDate"),
                        Sort.Order.desc("id")
                );
                case "deletedAtDesc" -> Sort.by(
                		Sort.Order.desc("deletedAt"),
                		Sort.Order.desc("id")
                );
                case "deletedAtAsc"  -> Sort.by(
                		Sort.Order.asc("deletedAt"),
                		Sort.Order.asc("id")
                );
                default -> Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                );
            };
            pageable = PageRequest.of(
                    Math.max(page, 0),
                    Math.min(Math.max(size, 1), 9),
                    sortObj
            );
        }

        // 로그인 사용자 정보
        UserEntity me = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));
        Long myDeptId = me.getDepartment().getId();

        // 팀장 여부 (삭제된 업무 권한 분기용)
        boolean isManager = me.getRole() == Role.MANAGER;

        Page<TaskEntity> result;

        if (usePrioritySort) {
            // 우선순위 정렬
        	
        	if (scope.equals("favorite")) {
                result = "priorityAsc".equals(sort)
                        ? favoriteRepository.findFavoriteTasksPriorityAsc(status, userId, deptId, pageable)
                        : favoriteRepository.findFavoriteTasksPriorityDesc(status, userId, deptId, pageable);
            } else {
        	 if (me.getRole() == Role.ADMIN) {
        	        // ADMIN → 전체 업무 우선순위 정렬
        	        result = "priorityAsc".equals(sort)
        	                ? taskRepository.findAllByPriorityMappedAsc(scope, status, userId, deptId, myDeptId, pageable)
        	                : taskRepository.findAllByPriorityMappedDesc(scope, status, userId, deptId, myDeptId, pageable);
        	    } else {
        	        // 일반 사용자/팀장 → 자신이 볼 수 있는 업무만 우선순위 정렬
        	        result = "priorityAsc".equals(sort)
        	                ? taskRepository.findVisibleTasksPriorityAsc(scope, status, userId, deptId, pageable)
        	                : taskRepository.findVisibleTasksPriorityDesc(scope, status, userId, deptId, pageable);
        	    }
            }
        } else {
            // 일반 scope 기반 조회
            if (me.getRole() == Role.ADMIN) {
                Long targetDeptId = deptId;

                switch(scope) {
                    case "all" -> {
                        if (targetDeptId != null) {
                            result = (status == null)
                                ? taskRepository.findByWorkDepartment_IdAndIsDeletedFalse(targetDeptId, pageable)
                                : taskRepository.findByWorkDepartment_IdAndStatusAndIsDeletedFalse(targetDeptId, status, pageable);
                        } else {
                            result = (status == null)
                                ? taskRepository.findByIsDeletedFalse(pageable)
                                : taskRepository.findByIsDeletedFalseAndStatus(status, pageable);
                        }
                    }

                    case "public" -> {
                        if (targetDeptId != null) {
                            result = (status == null)
                                ? taskRepository.findPublicByDepartmentId(targetDeptId, pageable)
                                : taskRepository.findPublicByDepartmentIdAndStatus(targetDeptId, status, pageable);
                        } else {
                            result = (status == null)
                                ? taskRepository.findPublicOnly(pageable)
                                : taskRepository.findPublicOnlyByStatus(status, pageable);
                        }
                    }

                    case "team" -> {
                        result = (status == null)
                            ? taskRepository.findByWorkDepartment_IdAndIsDeletedFalse(myDeptId, pageable)
                            : taskRepository.findByWorkDepartment_IdAndStatusAndIsDeletedFalse(myDeptId, status, pageable);
                    }

                    case "created" -> {
                        result = (status == null)
                            ? taskRepository.findByIsDeletedFalseAndCreatedBy_Id(userId, pageable)
                            : taskRepository.findByIsDeletedFalseAndCreatedBy_IdAndStatus(userId, status, pageable);
                    }

                    case "assigned" -> {
                        result = (status == null)
                            ? taskRepository.findByIsDeletedFalseAndAssignee_Id(userId, pageable)
                            : taskRepository.findByIsDeletedFalseAndAssignee_IdAndStatus(userId, status, pageable);
                    }

                    case "private" -> {
                        if (targetDeptId != null) {
                            result = (status == null)
                                ? taskRepository.findByIsDeletedFalseAndVisibilityAndWorkDepartment_Id(TaskVisibility.PRIVATE, targetDeptId, pageable)
                                : taskRepository.findByIsDeletedFalseAndVisibilityAndWorkDepartment_IdAndStatus(TaskVisibility.PRIVATE, targetDeptId, status, pageable);
                        } else {
                            result = (status == null)
                                ? taskRepository.findByIsDeletedFalseAndVisibility(TaskVisibility.PRIVATE, pageable)
                                : taskRepository.findByIsDeletedFalseAndVisibilityAndStatus(TaskVisibility.PRIVATE, status, pageable);
                        }
                    }

                    case "favorite" -> {
                        // 삭제된 업무는 즐겨찾기 대상이 아니므로 방어
                        if (status == null) {
                            result = switch(sort) {
                                case "createdAtAsc" -> favoriteRepository.findFavoriteTasksOrderByCreatedAtAsc(userId, deptId, pageable);
                                case "dueDateAsc" -> favoriteRepository.findFavoriteTasksOrderByDueDateAsc(userId, deptId, pageable);
                                case "dueDateDesc" -> favoriteRepository.findFavoriteTasksOrderByDueDateDesc(userId, deptId, pageable);
                                default -> favoriteRepository.findFavoriteTasksOrderByCreatedAtDesc(userId, deptId, pageable);
                            };
                        } else {
                            result = switch(sort) {
                                case "createdAtAsc" -> favoriteRepository.findFavoriteTasksByStatusOrderByCreatedAtAsc(userId, status, deptId, pageable);
                                case "dueDateAsc" -> favoriteRepository.findFavoriteTasksOrderByDueDateAsc(userId, deptId, pageable);
                                case "dueDateDesc" -> favoriteRepository.findFavoriteTasksOrderByDueDateDesc(userId, deptId, pageable);
                                default -> favoriteRepository.findFavoriteTasksByStatusOrderByCreatedAtDesc(userId, status, deptId, pageable);
                            };
                        }
                    }

                    case "deleted" -> {
                        // ADMIN은 전체 삭제 조회 가능
                        result = (status == null)
                            ? taskRepository.findAllDeleted(pageable)
                            : taskRepository.findAllDeletedByStatus(status, pageable);
                    }

                    default -> throw new ApiException(ErrorCode.BAD_REQUEST,
                            "scope 값이 올바르지 않습니다. (all|public|team|created|assigned|favorite|deleted)");
                }

            } else {
                // 일반 사용자/팀장
                switch (scope) {
                    case "all" -> result = (status == null)
                            ? taskRepository.findAllVisibleForUser(userId, myDeptId, pageable)
                            : taskRepository.findAllVisibleForUserByStatus(userId, myDeptId, status, pageable);

                    case "public" -> result = (status == null)
                            ? taskRepository.findPublicOnly(pageable)
                            : taskRepository.findPublicOnlyByStatus(status, pageable);

                    case "team" -> result = (status == null)
                            ? taskRepository.findTeamVisibleForUser(userId, myDeptId, pageable)
                            : taskRepository.findTeamVisibleForUserByStatus(userId, myDeptId, status, pageable);

                    case "created" -> result = (status == null)
                            ? taskRepository.findByIsDeletedFalseAndCreatedBy_Id(userId, pageable)
                            : taskRepository.findByIsDeletedFalseAndCreatedBy_IdAndStatus(userId, status, pageable);

                    case "assigned" -> result = (status == null)
                            ? taskRepository.findByIsDeletedFalseAndAssignee_Id(userId, pageable)
                            : taskRepository.findByIsDeletedFalseAndAssignee_IdAndStatus(userId, status, pageable);

                    case "private" -> result = (status == null)
                            ? taskRepository.findVisiblePrivateForUser(TaskVisibility.PRIVATE, userId, userId, pageable)
                            : taskRepository.findVisiblePrivateForUserByStatus(TaskVisibility.PRIVATE, status, userId, userId, pageable);

                    case "favorite" -> {
                        // 삭제된 업무는 즐겨찾기 대상이 아니므로 방어
                        if (status == null) {
                            result = switch(sort) {
                                case "createdAtAsc" -> favoriteRepository.findFavoriteTasksOrderByCreatedAtAsc(userId, deptId, pageable);
                                case "dueDateAsc" -> favoriteRepository.findFavoriteTasksOrderByDueDateAsc(userId, deptId, pageable);
                                case "dueDateDesc" -> favoriteRepository.findFavoriteTasksOrderByDueDateDesc(userId, deptId, pageable);
                                default -> favoriteRepository.findFavoriteTasksOrderByCreatedAtDesc(userId, deptId, pageable);
                            };
                        } else {
                            result = switch(sort) {
                                case "createdAtAsc" -> favoriteRepository.findFavoriteTasksByStatusOrderByCreatedAtAsc(userId, status, deptId, pageable);
                                case "dueDateAsc" -> favoriteRepository.findFavoriteTasksOrderByDueDateAsc(userId, deptId, pageable);
                                case "dueDateDesc" -> favoriteRepository.findFavoriteTasksOrderByDueDateDesc(userId, deptId, pageable);
                                default -> favoriteRepository.findFavoriteTasksByStatusOrderByCreatedAtDesc(userId, status, deptId, pageable);
                            };
                        }
                    }

                    case "deleted" -> result = (status == null)
                            ? taskRepository.findDeletedVisibleForUser(userId, isManager ? myDeptId : null, pageable)
                            : taskRepository.findDeletedVisibleForUserByStatus(userId, isManager ? myDeptId : null, status, pageable);

                    default -> throw new ApiException(ErrorCode.BAD_REQUEST,
                            "scope 값이 올바르지 않습니다. (all|public|team|created|assigned|favorite|deleted)");
                }
            }
        }

        // TaskEntity → TaskResponse 매핑, attachments count만 포함
        return result.map(t -> {
            long cnt;
            if (t.isDeleted()) {
                // 삭제된 업무도 포함
                cnt = attachmentService.countByTaskIncludingDeleted(t.getId());
            } else {
                cnt = attachmentService.countActiveByTask(t.getId());
            }
            
            // 즐겨찾기 엔티티 조회
            Optional<FavoriteEntity> favOpt = favoriteRepository.findByUserAndTask(me, t);
            boolean isFavorite = favOpt.isPresent();
            LocalDateTime favoriteCreatedAt = favOpt.map(FavoriteEntity::getCreatedAt).orElse(null);

            // TaskResponse DTO 반환, attachmentsCount + 즐겨찾기 여부 + 즐겨찾기 등록일
            return TaskResponse.from(t, List.of(), cnt, isFavorite, favoriteCreatedAt);
        });
    }

    // 업무 상세 조회
    public TaskResponse detail(Long taskId, Long userId) {

        // 로그인 체크
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 사용자 부서 ID
        UserEntity me = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));
        Long myDeptId = me.getDepartment().getId();

        // Task 조회 (삭제 포함 전체 조회)
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다."));

        // 삭제된 업무 여부 체크
        if (task.isDeleted()) {

            // 관리자면 전체 조회 가능
            if (me.getRole() != Role.ADMIN) {

                // 권한 체크 (작성자 / 담당자 / 같은 부서)
                boolean hasPermission =
                        task.getCreatedBy().getId().equals(userId) ||
                        (task.getAssignee() != null && task.getAssignee().getId().equals(userId)) ||
                        (myDeptId != null && task.getWorkDepartment().getId().equals(myDeptId));

                if (!hasPermission) {
                    throw new ApiException(ErrorCode.FORBIDDEN, "삭제된 업무 접근 권한이 없습니다.");
                }
            }

            // 삭제된 업무도 여기까지 통과하면 조회 허용 (읽기 전용)
        } else {

            // 삭제 안된 업무 → 기존 접근 제어 유지
            if (me.getRole() != Role.ADMIN) {
                taskRepository.findDetailVisibleForUser(taskId, userId, myDeptId)
                        .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다."));
            }
        }

     // 첨부 목록 로딩 (삭제 여부에 따라 분기)
        List<AttachmentResponse> attachments = task.isDeleted()
                // 삭제 포함 조회도 DTO로 맞춰야 함
                ? attachmentService.listAllByTaskIncludingDeleted(taskId)
                : attachmentService.listByTask(taskId);

        // 즐겨찾기 엔티티 조회
        Optional<FavoriteEntity> favOpt = favoriteRepository.findByUserAndTask(me, task);
        boolean isFavorite = favOpt.isPresent();
        LocalDateTime favoriteCreatedAt = favOpt.map(FavoriteEntity::getCreatedAt).orElse(null);

        // TaskResponse DTO 반환, 첨부 포함 + 즐겨찾기 등록일
        return TaskResponse.from(task, attachments, attachments.size(), isFavorite, favoriteCreatedAt);
    }

    // KPI 출력 + 즐겨찾기 추가
    public Map<String, Map<String, Long>> kpi(Long userId) {

        Map<String, Long> assigned = new LinkedHashMap<>();
        Map<String, Long> created = new LinkedHashMap<>();
        Map<String, Long> favorite = new LinkedHashMap<>();
        // LinkedHashMap: 삽입 순서 유지 (출력 순서 일관성)

        for (TaskStatus status : EnumSet.allOf(TaskStatus.class)) {
            // 내가 담당한 업무
            assigned.put(status.name(),
                    taskRepository.countByIsDeletedFalseAndAssignee_IdAndStatus(userId, status));

            // 내가 생성한 업무
            created.put(status.name(),
                    taskRepository.countByIsDeletedFalseAndCreatedBy_IdAndStatus(userId, status));

            // 내가 즐겨찾기한 업무 (status 기준으로 카운트)
            favorite.put(status.name(),
            		favoriteRepository.countByUserIdAndStatus(userId, status));
            // countByIsDeletedFalseAndFavorites_UserIdAndStatus는 repository에 맞게 정의 필요
        }

        return Map.of(
                "assigned", assigned,
                "created", created,
                "favorite", favorite
        );
    }

}
