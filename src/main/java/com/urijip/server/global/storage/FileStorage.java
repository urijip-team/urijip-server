package com.urijip.server.global.storage;

import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.stereotype.Component;

// S3에 파일을 올리고 지운다. 파일은 key(버킷 안의 경로)로 가리키고, DB에도 URL이 아닌 key를 저장한다.
@Component
@RequiredArgsConstructor
public class FileStorage {

	// S3 DeleteObjects 한 번에 지울 수 있는 최대 개수
	private static final int DELETE_BATCH_SIZE = 1000;

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final S3Properties properties;

	public void upload(String key, byte[] content, String contentType) {
		s3Client.putObject(request -> request.bucket(properties.bucket()).key(key).contentType(contentType),
				RequestBody.fromBytes(content));
	}

	public void deleteAll(Collection<String> keys) {
		List<ObjectIdentifier> objects = keys.stream()
				.map(key -> ObjectIdentifier.builder().key(key).build())
				.toList();
		for (int from = 0; from < objects.size(); from += DELETE_BATCH_SIZE) {
			List<ObjectIdentifier> batch = objects.subList(from, Math.min(from + DELETE_BATCH_SIZE, objects.size()));
			s3Client.deleteObjects(request -> request.bucket(properties.bucket())
					.delete(delete -> delete.objects(batch).quiet(true)));
		}
	}

	// 비공개 버킷의 파일을 잠시 볼 수 있는 서명 URL. key가 null이면 null을 돌려준다.
	public String createUrl(String key) {
		if (key == null) {
			return null;
		}
		return s3Presigner.presignGetObject(presign -> presign
				.signatureDuration(properties.urlExpiration())
				.getObjectRequest(request -> request.bucket(properties.bucket()).key(key)))
				.url()
				.toString();
	}

}
