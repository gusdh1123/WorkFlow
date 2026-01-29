package com.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//(exclude = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
public class WorkflowBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkflowBackendApplication.class, args);
		
		String gusdh = "sex";
		
	}

}
