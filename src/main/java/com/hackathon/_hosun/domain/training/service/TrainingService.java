package com.hackathon._hosun.domain.training.service;

import com.hackathon._hosun.domain.analysis.entity.AiReferenceDraft;
import com.hackathon._hosun.domain.analysis.entity.ComparisonAnalysis;
import com.hackathon._hosun.domain.analysis.event.AiReferenceDraftRequestedEvent;
import com.hackathon._hosun.domain.analysis.repository.AiReferenceDraftRepository;
import com.hackathon._hosun.domain.analysis.repository.ComparisonAnalysisRepository;
import com.hackathon._hosun.domain.training.dto.AttachmentResponse;
import com.hackathon._hosun.domain.training.dto.TrainingCreateRequest;
import com.hackathon._hosun.domain.training.dto.TrainingCreateResponse;
import com.hackathon._hosun.domain.training.dto.TrainingWorkspaceResponse;
import com.hackathon._hosun.domain.training.dto.TrainingWorkspaceResponse.EditorSource;
import com.hackathon._hosun.domain.training.dto.TrainingWorkspaceResponse.LatestVersion;
import com.hackathon._hosun.domain.training.entity.Training;
import com.hackathon._hosun.domain.training.entity.TrainingAttachment;
import com.hackathon._hosun.domain.training.repository.TrainingAttachmentRepository;
import com.hackathon._hosun.domain.training.repository.TrainingRepository;
import com.hackathon._hosun.domain.user.repository.UserRepository;
import com.hackathon._hosun.domain.writing.entity.WritingVersion;
import com.hackathon._hosun.domain.writing.repository.WritingVersionRepository;
import com.hackathon._hosun.global.exception.BusinessException;
import com.hackathon._hosun.global.exception.ErrorCode;
import com.hackathon._hosun.global.storage.FileStorage;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingService {

    // ponytail: 명세서 가정값(10MB·5개). 바뀌면 여기와 application.yaml multipart 설정 같이 수정
    private static final int MAX_ATTACHMENT_COUNT = 5;
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf",
            "ppt", "pptx",
            "doc", "docx", "hwp", "hwpx", "txt",
            "jpg", "jpeg", "png", "gif", "webp");

    private final TrainingRepository trainingRepository;
    private final TrainingAttachmentRepository trainingAttachmentRepository;
    private final UserRepository userRepository;
    private final WritingVersionRepository writingVersionRepository;
    private final ComparisonAnalysisRepository comparisonAnalysisRepository;
    private final AiReferenceDraftRepository aiReferenceDraftRepository;
    private final FileStorage fileStorage;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TrainingCreateResponse create(Long userId, TrainingCreateRequest request, List<MultipartFile> files) {
        List<MultipartFile> uploads = validateAttachments(files); //DB 저장 전에 파일 검증부터

        Training training = trainingRepository.save(Training.create(
                userRepository.getReferenceById(userId),
                request.topic().strip(),
                request.writingType(),
                StringUtils.hasText(request.requirements()) ? request.requirements().strip() : null,
                request.timerMinutes()));

        List<AttachmentResponse> attachments = uploads.stream()
                .map(file -> saveAttachment(training, file))
                .map(AttachmentResponse::from)
                .toList();

        // 과제 조건 + 첨부파일(저장소에서 다시 읽어 전달)로 AI 작성 글 생성. 커밋 이후 비동기로 실행됨 (AiReferenceDraftGenerator)
        AiReferenceDraft aiDraft = aiReferenceDraftRepository.save(AiReferenceDraft.createPending(training));
        eventPublisher.publishEvent(new AiReferenceDraftRequestedEvent(aiDraft.getId()));

        return new TrainingCreateResponse(
                training.getId(), training.getStatus(), training.getStartedAt(), training.getTimerEndsAt(), attachments);
    }

    public TrainingWorkspaceResponse getWorkspace(Long userId, Long trainingId) {
        Training training = trainingRepository.findByIdAndUser_IdAndDeletedAtIsNull(trainingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINING_NOT_FOUND));

        Optional<WritingVersion> latestVersion = writingVersionRepository.findTopByTraining_IdOrderBySeqNoDesc(trainingId);

        // 입력창 초기값: 임시 저장본 > 최근 제출본 > 빈 값
        String editorContent;
        EditorSource editorSource;
        if (training.getTempContent() != null) {
            editorContent = training.getTempContent();
            editorSource = EditorSource.TEMP;
        } else if (latestVersion.isPresent()) {
            editorContent = latestVersion.get().getContent();
            editorSource = EditorSource.VERSION;
        } else {
            editorContent = "";
            editorSource = EditorSource.EMPTY;
        }

        Long latestAnalysisId = comparisonAnalysisRepository.findTopByVersion_Training_IdOrderByCreatedAtDesc(trainingId)
                .map(ComparisonAnalysis::getId)
                .orElse(null);

        List<AttachmentResponse> attachments = trainingAttachmentRepository.findByTraining_IdOrderByIdAsc(trainingId)
                .stream()
                .map(AttachmentResponse::from)
                .toList();

        return new TrainingWorkspaceResponse(
                training.getId(),
                training.getTopic(),
                training.getWritingType(),
                training.getRequirements(),
                training.getStatus(),
                training.getTimerMinutes(),
                training.getStartedAt(),
                training.getTimerEndsAt(),
                editorContent,
                editorSource,
                training.getTempSavedAt(),
                latestVersion.map(v -> new LatestVersion(v.getId(), v.getSeqNo())).orElse(null),
                latestAnalysisId,
                attachments);
    }

    private List<MultipartFile> validateAttachments(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        // 파일 선택 없이 빈 파트가 오는 경우가 있어서 빈 파일은 무시
        List<MultipartFile> uploads = files.stream().filter(file -> !file.isEmpty()).toList();
        if (uploads.size() > MAX_ATTACHMENT_COUNT) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "첨부파일은 최대 " + MAX_ATTACHMENT_COUNT + "개까지 가능합니다.");
        }
        for (MultipartFile file : uploads) {
            if (file.getSize() > MAX_ATTACHMENT_SIZE) {
                throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
            }
            String extension = StringUtils.getFilenameExtension(originalFileName(file));
            if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }
        }
        return uploads;
    }

    private TrainingAttachment saveAttachment(Training training, MultipartFile file) {
        String fileUrl = fileStorage.store(file, "trainings/" + training.getId());
        deleteFileOnRollback(fileUrl);
        return trainingAttachmentRepository.save(TrainingAttachment.create(
                training, originalFileName(file), fileUrl, file.getContentType(), file.getSize()));
    }

    /** 이후 단계에서 실패해 DB가 롤백되면 이미 저장한 파일도 지워서 고아 파일이 남지 않게 한다 */
    private void deleteFileOnRollback(String fileUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    fileStorage.delete(fileUrl);
                }
            }
        });
    }

    /** 일부 브라우저는 전체 경로를 보내므로 파일명만 남긴다 */
    private String originalFileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "파일명이 없습니다.");
        }
        String fileName = Paths.get(name.replace('\\', '/')).getFileName().toString();
        if (fileName.length() > 255) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "파일명은 255자 이하여야 합니다.");
        }
        return fileName;
    }
}
