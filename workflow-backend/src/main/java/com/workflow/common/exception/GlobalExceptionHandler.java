package com.workflow.common.exception;

import com.workflow.common.response.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 우리가 만든 ApiException 처리
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException e) {

        ErrorCode errorCode = e.getErrorCode(); // 예외에서 ErrorCode 가져오기

        // ResponseEntity에 HTTP 상태, body(ApiError) 설정
        return ResponseEntity
                .status(errorCode.getStatus()) // HTTP 상태 코드 지정
                .body(ApiError.of(
                        errorCode.name(),        // 내부 코드 이름
                        e.getMessageToSend()     // 사용자/프론트에 전달할 메시지
                ));
    }

    // Spring Validation 예외 처리 (Controller @Valid 검증 실패)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {

        // TODO: 필요하면 실제 실패 필드 메시지를 파싱해서 상세 전달 가능
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getStatus())
                .body(ApiError.of(
                        ErrorCode.VALIDATION_ERROR.name(),
                        ErrorCode.VALIDATION_ERROR.getDefaultMessage()
                ));
    }

    // 그 외 처리되지 않은 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleEtc(Exception e) {

        // 주의: 실제 서비스에서는 내부 로깅 필요
        // e.printStackTrace() 혹은 Logger.error(e) 등으로 기록
        // 민감 정보가 외부로 노출되지 않도록 메시지는 일반화

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiError.of(
                        ErrorCode.INTERNAL_ERROR.name(),
                        ErrorCode.INTERNAL_ERROR.getDefaultMessage()
                ));
    }

    // 주의 사항:
    // 1. ApiException → 커스텀 메시지 + ErrorCode 기반 응답
    // 2. MethodArgumentNotValidException → 입력값 검증 실패 응답
    // 3. Exception → 예측 못한 서버 오류, 사용자에게는 INTERNAL_ERROR 메시지만 전달
    // 4. 필요 시 각 핸들러에서 로깅/모니터링, 알림 등 추가 가능
}
