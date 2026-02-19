package com.workflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 로그인 요청 전용 DTO
// 외부에서 들어오는 값만 정의
public record LoginRequest(

        // 이메일 형식 검증
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        // 공백 불가 + 최소 길이 제한
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 4, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        String password

) {}
