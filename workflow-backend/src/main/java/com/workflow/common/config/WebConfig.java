package com.workflow.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 스프링 MVC 설정 클래스
// 정적 리소스 매핑(파일 접근 경로)을 커스터마이징하기 위해 사용
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // application.yml 또는 application.properties 에서
    // app.upload-dir 경로 값을 주입받음
    // 예: app.upload-dir=C:/upload
    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // /uploads/** 로 들어오는 요청을
        // 실제 서버의 파일 시스템 경로로 연결해주는 설정
        // 즉, DB가 아니라 "물리 파일"을 직접 읽어서 응답

        registry.addResourceHandler("/uploads/**")

                // file: 접두어는 "로컬 파일 시스템 경로"라는 의미
                // uploadDir 경로 하위 파일들을 브라우저에서 접근 가능하게 함
                // 마지막 "/" 필수 (디렉토리 기준 매핑)
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
