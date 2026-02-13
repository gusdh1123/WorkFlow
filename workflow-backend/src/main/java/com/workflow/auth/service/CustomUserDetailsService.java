package com.workflow.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.workflow.user.entity.UserEntity;
import com.workflow.user.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String userIdStr) {
	  
    Long userId;
    
    try {
    	
      userId = Long.parseLong(userIdStr);
      
    } catch (NumberFormatException e) {
    	
      throw new UsernameNotFoundException(userIdStr);
    }

    UserEntity u = userRepository.findById(userId)
        .orElseThrow(() -> new UsernameNotFoundException(userIdStr));

//    System.out.println("[UDS] found id=" + u.getId()
//    + ", email=" + u.getEmail()
//    + ", pw=" + (u.getPassword() == null ? "null" : "len=" + u.getPassword().length()));

    // 권한은 u.getRole() 기반 추천
    // "USER" / "ADMIN" 형태면 roles()로 넣기
    // "ROLE_USER" 형태면 authorities()로 넣어야 함: 얘는 권한을 더 강화 시킬때 어드민만 삭제, 수정 이런식으로 특정 기능 권한("USER_READ", "USER_WRITE", "ADMIN_DELETE")
    return org.springframework.security.core.userdetails.User
        .withUsername(String.valueOf(u.getId())) // principal username을 id로 맞춤(일관성)
        .password(u.getPassword())
        .roles(u.getRole().name())
        .build();
  }
}