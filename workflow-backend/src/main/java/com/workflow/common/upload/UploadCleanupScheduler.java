package com.workflow.common.upload;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 업로드 tmp 폴더 정리 스케줄러
// 일정 시간 지난 임시 파일들을 자동으로 삭제
@Component
public class UploadCleanupScheduler {

    private final Path root;      // 업로드 루트 디렉토리
    private final long ttlHours;  // 파일 보존 시간(시 단위)

    // 청소 대상 모듈 추가 가능, 필요 시 "profile" 등 더 넣으면 됨
    private static final List<String> MODULES = List.of(
            "tasks"
    );

    public UploadCleanupScheduler(
            @Value("${app.upload-dir}") String uploadDir,
            @Value("${app.upload-tmp-ttl-hours:24}") long ttlHours // 기본 24시간
    ) {
        this.root = Paths.get(uploadDir).normalize(); 
        this.ttlHours = ttlHours;
    }

    // 10분마다 실행, tmp 파일 정리
    @Scheduled(fixedDelayString = "PT10M") 
    public void cleanupOldTmpFiles() {

        // 삭제 기준 시각 = 현재 시각 - ttlHours
        Instant cutoff = Instant.now().minus(ttlHours, ChronoUnit.HOURS);

        for (String module : MODULES) {

            // module 별 tmp 폴더 경로
            Path tmpDir = root.resolve(Paths.get(module, "tmp")).normalize();

            if (!Files.exists(tmpDir)) continue; // 폴더 없으면 스킵

            try (Stream<Path> paths = Files.list(tmpDir)) {

                paths.filter(Files::isRegularFile).forEach(p -> {
                    try {
                        FileTime lastModified = Files.getLastModifiedTime(p);

                        // 수정 시간이 기준 시각 이전이면 삭제
                        if (lastModified.toInstant().isBefore(cutoff)) {
                            Files.deleteIfExists(p);
                        }

                    } catch (IOException ignored) {
                        // 개별 파일 삭제 실패시 무시
                    }
                });

            } catch (IOException ignored) {
                // 디렉토리 접근 실패시 무시
            }
        }
    }
}
