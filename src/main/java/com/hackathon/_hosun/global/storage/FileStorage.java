package com.hackathon._hosun.global.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장소 추상화. 현재는 LocalFileStorage만 있고, S3 연동 시 구현체를 추가하고
 * file.storage.type=s3 로 바꾸면 된다.
 */
public interface FileStorage {

    /**
     * @param file      업로드 파일
     * @param directory 저장 디렉터리(키 prefix), 예: "trainings/12"
     * @return 저장된 파일의 접근 URL (DB의 file_url에 저장)
     */
    String store(MultipartFile file, String directory);

    /** store()가 돌려준 URL로 파일을 삭제한다. 없는 파일이면 무시 */
    void delete(String fileUrl);
}
