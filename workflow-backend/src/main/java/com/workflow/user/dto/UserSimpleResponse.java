package com.workflow.user.dto;

public record UserSimpleResponse(
        Long id,
        String name,
        String department
) {}
