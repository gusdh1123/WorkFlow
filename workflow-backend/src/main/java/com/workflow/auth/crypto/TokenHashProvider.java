package com.workflow.auth.crypto;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


//해시값 변경 로직
// SHA_256 -> 시크릿키 없이도 입력 = 같은 결과
// HMAC_SHA_256 -> 시크릿키 없으면 해시만들기 불가
@Component
public class TokenHashProvider {

    private final String hashSecret;

    public TokenHashProvider(@Value("${hasher.secret}") String hashSecret) {
        this.hashSecret = hashSecret;
    }

    public String hashRefreshToken(String refreshToken) {
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, hashSecret).hmacHex(refreshToken);
    }
}
