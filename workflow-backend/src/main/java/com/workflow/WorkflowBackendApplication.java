package com.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // 스케줄러 활성화 (Cron, FixedDelay 등 사용 가능)
@SpringBootApplication
// Security 자동 설정 제외가 필요하면 아래 주석 해제 가능
// (exclude = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
public class WorkflowBackendApplication {

    public static void main(String[] args) {
        // Spring Boot 애플리케이션 실행
        SpringApplication.run(WorkflowBackendApplication.class, args);
    }
}
