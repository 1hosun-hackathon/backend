package com.hackathon._hosun.global.storage;

import com.hackathon._hosun.global.exception.BusinessException;
import com.hackathon._hosun.global.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로컬 디스크 저장소. {base-dir}/{directory}/{uuid}.{ext} 로 저장하고 {url-prefix}/{directory}/{uuid}.{ext} 를 URL로 돌려준다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "file.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path baseDir;
    private final String urlPrefix;

    public LocalFileStorage(
            @Value("${file.storage.local.base-dir}") String baseDir,
            @Value("${file.storage.local.url-prefix}") String urlPrefix) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
    }

    @Override
    public String store(MultipartFile file, String directory) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String key = directory + "/" + UUID.randomUUID() + (extension == null ? "" : "." + extension.toLowerCase());
        Path target = resolve(key);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", target, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return urlPrefix + "/" + key;
    }

    @Override
    public byte[] load(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(urlPrefix + "/")) {
            throw new IllegalArgumentException("이 저장소의 파일 URL이 아닙니다: " + fileUrl);
        }
        Path target = resolve(fileUrl.substring(urlPrefix.length() + 1));
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 읽기 실패: " + target, e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(urlPrefix + "/")) {
            return;
        }
        Path target = resolve(fileUrl.substring(urlPrefix.length() + 1));
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {}", target, e);
        }
    }

    private Path resolve(String key) {
        Path path = baseDir.resolve(key).normalize();
        if (!path.startsWith(baseDir)) { // ../ 등으로 base-dir 밖을 가리키는 키 차단
            throw new IllegalArgumentException("잘못된 파일 경로입니다: " + key);
        }
        return path;
    }
}
