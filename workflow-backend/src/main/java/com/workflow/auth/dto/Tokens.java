package com.workflow.auth.dto;
// record: 데이터만 담는 불변 객체 DTO를 아주 간단하게 만드는 문법
// private final 필드
// 전체 생성자
// getter (accessToken(), refreshToken())
// equals()
// hashCode()
// toString()
// 를 자동으로 만들어 줌.
// 단, 생성 후 값 변경 불가
// 토큰, 응답 등 값이 변경 안되는 객체에 최고로 안전.
// Jackson / Spring 기본 지원

//record는 불변이라서 멀티스레드 환경에서도 안전
//토큰처럼 절대 변경되면 안되는 값에 적합
//setter가 없어서 실수로 값 바뀌는 사고 방지 가능

public record Tokens(String accessToken, String refreshToken) {}

