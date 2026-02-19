package com.workflow.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

// 공통 페이징 응답 DTO
// Spring Data JPA의 Page<T> 객체를
// 프론트에 전달하기 좋은 형태로 변환하기 위한 래퍼(record)
public record PageResponse<T>(

        // 실제 데이터 목록
        List<T> content,

        // 현재 페이지 번호 (0부터 시작)
        int page,

        // 한 페이지에 들어가는 데이터 개수
        int size,

        // 전체 데이터 개수 (DB 전체 row 수)
        long totalElements,

        // 전체 페이지 수
        int totalPages,

        // 현재 페이지가 첫 페이지인지 여부
        boolean first,

        // 현재 페이지가 마지막 페이지인지 여부
        boolean last

) {

    // Spring의 Page<T> 객체를
    // 우리가 정의한 PageResponse<T>로 변환하는 정적 팩토리 메서드
    public static <T> PageResponse<T> from(Page<T> p) {

        return new PageResponse<>(

                // 현재 페이지 데이터
                p.getContent(),

                // 현재 페이지 번호 (0-base)
                p.getNumber(),

                // 페이지 크기
                p.getSize(),

                // 전체 데이터 수
                p.getTotalElements(),

                // 전체 페이지 수
                p.getTotalPages(),

                // 첫 페이지 여부
                p.isFirst(),

                // 마지막 페이지 여부
                p.isLast()
        );
    }
}
