package com.workflow.user.dto;

// 최소 사용자 정보 전송용 DTO
// 업무 담당자 선택용 리스트 등에서 사용
public record UserSimpleResponse(
        Long id,           // 사용자 고유 ID
        String name,       // 사용자 이름
        String department  // 소속 부서명
) {}
