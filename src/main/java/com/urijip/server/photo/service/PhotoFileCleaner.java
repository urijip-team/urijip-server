package com.urijip.server.photo.service;

import java.util.Collection;

import com.urijip.server.global.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// S3 파일 삭제는 DB 작업이 이미 끝난 뒤라 실패해도 요청을 실패시키지 않는다. 남은 파일은 로그로 남긴다.
@Slf4j
@Component
@RequiredArgsConstructor
public class PhotoFileCleaner {

	private final FileStorage fileStorage;

	@TransactionalEventListener
	public void onPhotoFilesDeleted(PhotoFilesDeletedEvent event) {
		deleteQuietly(event.keys());
	}

	public void deleteQuietly(Collection<String> keys) {
		if (keys.isEmpty()) {
			return;
		}
		try {
			fileStorage.deleteAll(keys);
		} catch (RuntimeException e) {
			log.warn("Failed to delete photo files: {}", keys, e);
		}
	}

}
