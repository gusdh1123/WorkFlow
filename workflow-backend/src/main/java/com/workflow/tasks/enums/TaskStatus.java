package com.workflow.tasks.enums;

// 업무 상태
public enum TaskStatus {

    TODO,         // 새로 생성된 업무, 시작 전 상태
    IN_PROGRESS,  // 진행 중인 업무
    REVIEW,       // 검토/승인 대기 중인 업무
    DONE,         // 완료된 업무
    ON_HOLD,      // 일시 중지된 업무
    CANCELED      // 취소된 업무

}
