package com.workflow.common.exception;

public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode; // 예외와 연결된 내부 코드 + HTTP 상태 정보
    private final String customMessage; // 필요 시 사용자 정의 메시지

    // 기본 메시지 사용
    public ApiException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage()); // RuntimeException 기본 메시지에도 넣어둠
        this.errorCode = errorCode;
        this.customMessage = null; // 커스텀 메시지가 없으면 기본 메시지를 사용
    }

    // 메시지 커스터마이징
    public ApiException(ErrorCode errorCode, String customMessage) {
        super(customMessage); // RuntimeException 기본 메시지에도 커스텀 메시지 저장
        this.errorCode = errorCode;
        this.customMessage = customMessage; // 실제 사용자/프론트로 내려줄 메시지
    }

    public ErrorCode getErrorCode() {
        return errorCode; // 예외 처리 핸들러에서 HTTP 상태 및 내부 코드 확인용
    }

    // 실제 내려줄 메시지
    public String getMessageToSend() {
        // 커스텀 메시지가 있으면 우선, 없으면 ErrorCode 기본 메시지 사용
        return customMessage != null
                ? customMessage
                : errorCode.getDefaultMessage();
    }

    // 주의: RuntimeException 상속이므로 체크 예외가 아님
    // → 필요 시 try-catch 없이도 throw 가능, 글로벌 예외 핸들러에서 처리 권장
}
