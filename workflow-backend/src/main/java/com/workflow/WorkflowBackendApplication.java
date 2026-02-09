package com.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// 스케줄러 활성화
@EnableScheduling
@SpringBootApplication
// (exclude = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
public class WorkflowBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkflowBackendApplication.class, args);
	}

}
