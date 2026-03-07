package com.workflow.tasks.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.time.LocalDateTime;

import jakarta.annotation.PostConstruct; // javax -> jakarta
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.attachment.repository.AttachmentRepository;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.repasitory.TaskRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SoftDeletedTaskCleanupScheduler {

    private final TaskRepository taskRepository;
    private final AttachmentRepository attachmentRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.task-cleanup.retention-days:14}")
    private int retentionDays;

//    @Value("${app.task-cleanup.retention-minutes:1}") // 테스트용
//    private int retentionMinutes;

    private Path root;

    public SoftDeletedTaskCleanupScheduler(TaskRepository taskRepository,
                                           AttachmentRepository attachmentRepository) {
        this.taskRepository = taskRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @PostConstruct
    public void init() {
        this.root = Paths.get(uploadDir).normalize();
    }

    // 테스트용 10초마다 실행
    // @Scheduled(fixedDelay = 10000)
    // 매일 새벽 3시 실행
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupSoftDeletedTasks() {
        List<TaskEntity> deletedTasks = taskRepository.findByIsDeletedTrue();

        for (TaskEntity task : deletedTasks) {
            if (task.getDeletedAt() == null ||
                task.getDeletedAt().plusDays(retentionDays).isAfter(LocalDateTime.now())) {
                continue; // 아직 보존 기간 안 지남
            }

            // 파일 삭제
            Path taskDir = root.resolve(Paths.get("tasks", task.getId().toString())).normalize();
            if (Files.exists(taskDir) && Files.isDirectory(taskDir)) {
                try (Stream<Path> paths = Files.walk(taskDir)) {
                    paths.sorted(Comparator.reverseOrder())
                         .forEach(p -> {
                             try { Files.deleteIfExists(p); } 
                             catch (IOException e) {
                            	 }
                         });
                } catch (IOException e) {
                }
            }

            // 첨부파일 DB 삭제
            attachmentRepository.deleteByTaskId(task.getId());

            // Task DB 삭제
            taskRepository.delete(task);

        }
    }
}