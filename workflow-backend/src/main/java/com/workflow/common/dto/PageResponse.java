package com.workflow.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        // 데이터 개수
        long totalElements,
        int totalPages,
        // 현재 페이지가 첫 페이지냐
        boolean first,
        // 현재 페이지가 마지막 페이지냐
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isFirst(),
                p.isLast()
        );
    }
}
