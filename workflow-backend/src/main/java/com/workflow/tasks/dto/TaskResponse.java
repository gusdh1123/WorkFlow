package com.workflow.tasks.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.workflow.attachment.dto.AttachmentResponse;
import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.enums.TaskVisibility;
import com.workflow.user.entity.UserEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskResponse {

    private Long id;                      // 업무 ID
    private String title;                  // 업무 제목
    private String description;            // 업무 상세 내용
    private TaskStatus status;             // 상태 (TODO, IN_PROGRESS, DONE 등)
    private TaskPriority priority;         // 우선순위
    private TaskVisibility visibility;     // 공개 범위 (PUBLIC, DEPARTMENT, PRIVATE 등)
    private LocalDate dueDate;             // 마감일
    private LocalDateTime createdAt;       // 생성 시각

    private Long createdById;              // 생성자 ID
    private String createdByName;          // 생성자 이름
    private String createdByDepartmentName;// 생성자 소속 부서명
    private String createdByDepartmentCode;// 생성자 소속 부서 코드

    private Long assigneeId;               // 담당자 ID
    private String assigneeName;           // 담당자 이름
    private String assigneeDepartmentName; // 담당자 소속 부서명
    private String assigneeDepartmentCode; // 담당자 소속 부서 코드

    private String ownerDepartmentName;    // 업무 소유 부서명
    private String ownerDepartmentCode;    // 업무 소유 부서 코드
    private String workDepartmentName;     // 실제 업무 처리 부서명
    private String workDepartmentCode;     // 실제 업무 처리 부서 코드

    private List<AttachmentResponse> attachments; // 첨부파일 리스트
    private long attachmentsCount;                // 첨부파일 개수, 목록/상세 공통

    // 정적 빌더 변환 메서드
    
    // 상세 조회용: 첨부 리스트 포함
    public static TaskResponse from(TaskEntity t, List<AttachmentResponse> attachments) {
        long count = (attachments == null) ? 0 : attachments.size();
        return from(t, attachments, count);
    }

    // 목록 조회용: 첨부 리스트 없이 개수만
    public static TaskResponse from(TaskEntity t, long attachmentsCount) {
        return from(t, List.of(), attachmentsCount);
    }

    // 공통 빌더: 생성자, 담당자, 부서 정보 모두 포함
    public static TaskResponse from(TaskEntity t,
                                       List<AttachmentResponse> attachments,
                                       long attachmentsCount) {

        UserEntity cb = t.getCreatedBy();  // 생성자 정보
        UserEntity as = t.getAssignee();   // 담당자 정보

        DepartmentEntity cbDept = (cb == null) ? null : cb.getDepartment(); // 생성자 부서
        DepartmentEntity asDept = (as == null) ? null : as.getDepartment(); // 담당자 부서

        DepartmentEntity ownerDept = t.getOwnerDepartment(); // 업무 소유 부서
        DepartmentEntity workDept = t.getWorkDepartment();  // 실제 처리 부서

        return TaskResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus())
                .priority(t.getPriority())
                .visibility(t.getVisibility())
                .dueDate(t.getDueDate())
                .createdAt(t.getCreatedAt())

                .createdById(cb != null ? cb.getId() : null)
                .createdByName(cb != null ? cb.getName() : null)
                .createdByDepartmentName(cbDept != null ? cbDept.getName() : null)
                .createdByDepartmentCode(cbDept != null ? cbDept.getCode() : null)

                .assigneeId(as != null ? as.getId() : null)
                .assigneeName(as != null ? as.getName() : null)
                .assigneeDepartmentName(asDept != null ? asDept.getName() : null)
                .assigneeDepartmentCode(asDept != null ? asDept.getCode() : null)

                .ownerDepartmentName(ownerDept != null ? ownerDept.getName() : null)
                .ownerDepartmentCode(ownerDept != null ? ownerDept.getCode() : null)
                .workDepartmentName(workDept != null ? workDept.getName() : null)
                .workDepartmentCode(workDept != null ? workDept.getCode() : null)

                .attachments(attachments != null ? attachments : List.of())
                .attachmentsCount(Math.max(0, attachmentsCount)) // 음수 방어
                .build();
    }

    // 기존 코드 호환용: 첨부파일 0개 처리
    public static TaskResponse from(TaskEntity t) {
        return from(t, 0);
    }
}
