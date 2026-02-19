package com.workflow.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.workflow.user.entity.UserEntity;
import com.workflow.user.repository.UserRepository;

// Spring Security 인증 시 사용하는 UserDetailsService 구현체
// 로그인 요청이 오면 Security가 내부적으로 loadUserByUsername() 호출
// 여기서 반환한 UserDetails 정보를 기반으로 비밀번호 검증 진행
// 우리는 username 대신 userId(Long)를 사용하는 구조
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  // Spring Security 인증 과정에서 호출되는 핵심 메서드
  // 파라미터는 "username" 이지만 우리는 userId를 문자열로 받아 사용
  // 반드시 UsernameNotFoundException을 던져야 인증 흐름이 정상 동작
  @Override
  public UserDetails loadUserByUsername(String userIdStr) {
	  
    Long userId;
    
    try {
    	
      // username 대신 userId(Long)를 쓰기 때문에
      // 전달받은 문자열을 Long으로 변환
      userId = Long.parseLong(userIdStr);
      
    } catch (NumberFormatException e) {
    	
      // 숫자 변환 실패 = 존재하지 않는 사용자로 처리
      // 보안상 구체적인 실패 이유는 노출하지 않음
      throw new UsernameNotFoundException(userIdStr);
    }

    // DB에서 사용자 조회
    // 존재하지 않으면 인증 실패 처리
    UserEntity u = userRepository.findById(userId)
        .orElseThrow(() -> new UsernameNotFoundException(userIdStr));

    //    System.out.println("[UDS] found id=" + u.getId()
    //    + ", email=" + u.getEmail()
    //    + ", pw=" + (u.getPassword() == null ? "null" : "len=" + u.getPassword().length()));

    // 권한은 u.getRole() 기반 추천
    // "USER" / "ADMIN" 형태면 roles()로 넣기
    // "ROLE_USER" 형태면 authorities()로 넣어야 함: 얘는 권한을 더 강화 시킬때 어드민만 삭제, 수정 이런식으로 특정 기능 권한("USER_READ", "USER_WRITE", "ADMIN_DELETE")

    // Security에서 사용할 인증 객체 생성
    // withUsername() 자리에 userId를 넣어서 전체 인증 흐름을 id 기준으로 통일
    return org.springframework.security.core.userdetails.User
        .withUsername(String.valueOf(u.getId())) // principal username을 id로 맞춤(일관성)
        .password(u.getPassword()) // DB에 저장된 암호화된 비밀번호
        .roles(u.getRole().name()) // roles()는 내부적으로 ROLE_ 접두어를 자동 추가
        .build();
  }
}
