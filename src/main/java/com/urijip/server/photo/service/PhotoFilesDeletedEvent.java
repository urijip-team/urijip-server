package com.urijip.server.photo.service;

import java.util.List;

// 사진 행을 지운 트랜잭션이 커밋되면 S3 파일도 지운다. 롤백되면 파일은 그대로 둔다.
public record PhotoFilesDeletedEvent(List<String> keys) {
}
