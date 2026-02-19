package com.workflow.auth.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Refresh Token을 그대로 DB에 저장하지 않고 암호화해서 저장하기 위한 클래스
// 해시값 변경 로직
// SHA_256 -> 시크릿키 없이도 입력 = 같은 결과
// HMAC_SHA_256 -> 시크릿키 없으면 해시만들기 불가
// @Component: Spring bean으로 등록됨 -> 어디서든 @Autowired로 주입 가능
@Component
public class TokenHashProvider {

    // 시크릿키는 외부에서 주입받아서 사용 (절대 코드에 직접 쓰면 안됨)
    private final HmacUtils hmacUtils;

    // 생성자에서 @Value 주입
	// properties에서 해쉬 시크릿 값 가져옴.
    public TokenHashProvider(@Value("${hasher.secret}") String hashSecret) {
    	
        // HmacUtils 객체는 한번만 생성해서 재사용 (매번 new 하면 불필요한 객체 생성됨)
        this.hmacUtils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, hashSecret);
    }

    // HMAC_SHA_256 알고리즘
    // hashSecret을 키로 사용
    // refreshToken을 입력값으로 HMAC 계산
    // 결과를 hex 문자열로 반환
    public String hashRefreshToken(String refreshToken) {
    	
        return hmacUtils.hmacHex(refreshToken);
    }

    // DB에 저장된 해시값과 비교할 때 사용하는 메서드
    // equals() 쓰면 문자열 앞부분부터 비교해서 타이밍 공격에 취약할 수 있음
    // MessageDigest.isEqual() 사용하면 constant-time 비교라서 보안적으로 더 안전함
    public boolean matches(String rawToken, String storedHash) {
    	
        String calculatedHash = hashRefreshToken(rawToken);

        return MessageDigest.isEqual(
                calculatedHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
    
    // 해시 저장하는 이유
    // DB 털리면 공격자가 토큰 그대로 사용 가능하므로 즉시 계정이 탈취당함.
}
