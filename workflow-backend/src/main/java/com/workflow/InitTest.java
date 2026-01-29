package com.workflow;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InitTest {
	
	private final TestRepository testRepository;
	
	@PostConstruct
	public void init() {
		Test test = new Test();
		testRepository.save(test);
	}

}
