package com.workflow.common.exception;

import org.springframework.http.HttpStatus;


// 204 : 데이터 문제
public class NotFoundException extends ApiException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
