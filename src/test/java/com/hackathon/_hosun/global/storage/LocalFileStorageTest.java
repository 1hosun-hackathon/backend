package com.hackathon._hosun.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageTest {

    @TempDir
    Path baseDir;

    @Test
    void 저장한_파일을_URL로_다시_읽는다() {
        LocalFileStorage storage = new LocalFileStorage(baseDir.toString(), "/files");
        byte[] data = "탄소세 자료".getBytes(StandardCharsets.UTF_8);

        String url = storage.store(new MockMultipartFile("files", "자료.txt", "text/plain", data), "trainings/1");

        assertThat(storage.load(url)).isEqualTo(data);
    }

    @Test
    void 없는_파일이나_다른_저장소_URL은_읽지_못한다() {
        LocalFileStorage storage = new LocalFileStorage(baseDir.toString(), "/files");

        assertThatThrownBy(() -> storage.load("/files/trainings/1/none.txt")).isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(() -> storage.load("https://bucket/x.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.load("/files/../secret.txt")).isInstanceOf(IllegalArgumentException.class);
    }
}
