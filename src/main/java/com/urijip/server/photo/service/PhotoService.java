package com.urijip.server.photo.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.urijip.server.family.service.FamilyDeletedEvent;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.global.storage.FileStorage;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.service.UserService;
import com.urijip.server.photo.dto.request.PhotoPageRequest;
import com.urijip.server.photo.dto.response.PhotoResponse;
import com.urijip.server.photo.entity.Album;
import com.urijip.server.photo.entity.Photo;
import com.urijip.server.photo.repository.AlbumRepository;
import com.urijip.server.photo.repository.PhotoCommentRepository;
import com.urijip.server.photo.repository.PhotoRepository;
import com.urijip.server.photo.service.ImageProcessor.ProcessedImage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PhotoService {

	static final int MAX_UPLOAD_COUNT = 10;

	private final PhotoRepository photoRepository;
	private final AlbumRepository albumRepository;
	private final PhotoCommentRepository photoCommentRepository;
	private final AlbumService albumService;
	private final UserService userService;
	private final ImageProcessor imageProcessor;
	private final FileStorage fileStorage;
	private final PhotoFileCleaner photoFileCleaner;
	private final ApplicationEventPublisher eventPublisher;
	private final TransactionTemplate transactionTemplate;

	@Transactional(readOnly = true)
	public PageResponse<PhotoResponse> getPhotos(Long albumId, Long userId, PhotoPageRequest request) {
		albumService.getAccessibleAlbum(albumId, userId);

		PageRequest pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault(),
				Sort.by(Sort.Direction.DESC, "id"));
		return PageResponse.from(photoRepository.findByAlbumId(albumId, pageable).map(this::toResponse));
	}

	// S3 업로드는 오래 걸릴 수 있어 트랜잭션 밖에서 한다. 파일을 모두 올린 뒤 짧은 트랜잭션으로 행을 저장하고,
	// 중간에 실패하면 이미 올린 파일을 지운다.
	public List<PhotoResponse> upload(Long albumId, Long userId, List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		if (files.size() > MAX_UPLOAD_COUNT) {
			throw new BusinessException(ErrorCode.TOO_MANY_PHOTOS);
		}
		Long familyId = albumService.getAccessibleAlbum(albumId, userId).getFamily().getId();

		List<String> uploadedKeys = new ArrayList<>();
		try {
			List<UploadedPhoto> uploaded = new ArrayList<>();
			for (MultipartFile file : files) {
				uploaded.add(uploadFile(familyId, albumId, imageProcessor.process(file), uploadedKeys));
			}
			return transactionTemplate.execute(status -> savePhotos(albumId, userId, uploaded));
		} catch (RuntimeException e) {
			photoFileCleaner.deleteQuietly(uploadedKeys);
			throw e;
		}
	}

	// 올린 사람만 지울 수 있다.
	@Transactional
	public void delete(Long photoId, Long userId) {
		Photo photo = getAccessiblePhoto(photoId, userId);
		if (!photo.getUploader().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.NOT_PHOTO_UPLOADER);
		}

		remove(photo);
	}

	// 관리자가 신고된 사진을 지울 때 쓴다. 올린 사람인지 보지 않으며, 이미 없는 사진이면 아무것도 하지 않는다.
	@Transactional
	public void deleteByAdmin(Long photoId) {
		photoRepository.findWithAlbumById(photoId).ifPresent(this::remove);
	}

	// 관리자가 신고된 사진을 볼 때 쓴다. 가족 멤버인지 보지 않으며, 없는 id는 건너뛴다.
	@Transactional(readOnly = true)
	public List<PhotoResponse> getPhotos(Collection<Long> photoIds) {
		return photoRepository.findByIdIn(photoIds).stream().map(this::toResponse).toList();
	}

	// 사진이 없으면 404, 요청자가 사진이 있는 가족의 멤버가 아니면 403.
	@Transactional(readOnly = true)
	public Photo getAccessiblePhoto(Long photoId, Long userId) {
		Photo photo = photoRepository.findWithAlbumById(photoId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_NOT_FOUND));
		albumService.getAccessibleAlbum(photo.getAlbum().getId(), userId);
		return photo;
	}

	// 댓글은 사진을 참조하므로 먼저 지운다. albums와 photos가 서로를 참조하므로 대표 사진을 먼저 비운다.
	// Hibernate는 UPDATE를 DELETE보다 먼저 실행한다.
	@EventListener
	public void onFamilyDeleted(FamilyDeletedEvent event) {
		photoCommentRepository.deleteByFamilyId(event.familyId());
		List<Album> albums = albumRepository.findByFamilyId(event.familyId());
		List<Photo> photos = photoRepository.findByAlbumFamilyId(event.familyId());
		List<String> keys = photos.stream()
				.flatMap(photo -> Stream.of(photo.getImageKey(), photo.getThumbnailKey()))
				.toList();

		albums.forEach(album -> album.changeCoverPhoto(null));
		photoRepository.deleteAll(photos);
		albumRepository.deleteAll(albums);

		eventPublisher.publishEvent(new PhotoFilesDeletedEvent(keys));
	}

	// 대표 사진이면 남은 사진 중 가장 최근 것이 대표가 된다. 댓글도 함께 지우고, S3 파일은 커밋 뒤에 지운다.
	private void remove(Photo photo) {
		Album album = photo.getAlbum();
		if (album.getCoverPhoto() != null && album.getCoverPhoto().getId().equals(photo.getId())) {
			album.changeCoverPhoto(photoRepository.findFirstByAlbumIdAndIdNotOrderByIdDesc(album.getId(),
					photo.getId()).orElse(null));
		}
		photoCommentRepository.deleteByPhotoId(photo.getId());
		photoRepository.delete(photo);

		eventPublisher.publishEvent(new PhotoFilesDeletedEvent(List.of(photo.getImageKey(), photo.getThumbnailKey())));
	}

	private UploadedPhoto uploadFile(Long familyId, Long albumId, ProcessedImage image, List<String> uploadedKeys) {
		String baseKey = "families/%d/albums/%d/%s".formatted(familyId, albumId, UUID.randomUUID());
		String extension = image.format().extension();
		String imageKey = baseKey + "." + extension;
		String thumbnailKey = baseKey + "_thumb." + extension;

		fileStorage.upload(imageKey, image.original(), image.format().contentType());
		uploadedKeys.add(imageKey);
		fileStorage.upload(thumbnailKey, image.thumbnail(), image.format().contentType());
		uploadedKeys.add(thumbnailKey);

		return new UploadedPhoto(imageKey, thumbnailKey);
	}

	private List<PhotoResponse> savePhotos(Long albumId, Long userId, List<UploadedPhoto> uploaded) {
		Album album = albumService.getAccessibleAlbum(albumId, userId);
		User uploader = userService.getUser(userId);

		List<Photo> photos = photoRepository.saveAll(uploaded.stream()
				.map(file -> Photo.builder()
						.album(album)
						.uploader(uploader)
						.imageKey(file.imageKey())
						.thumbnailKey(file.thumbnailKey())
						.build())
				.toList());
		if (album.getCoverPhoto() == null) {
			album.changeCoverPhoto(photos.getFirst());
		}

		return photos.stream().map(this::toResponse).toList();
	}

	private PhotoResponse toResponse(Photo photo) {
		return PhotoResponse.of(photo, fileStorage.createUrl(photo.getThumbnailKey()),
				fileStorage.createUrl(photo.getImageKey()));
	}

	private record UploadedPhoto(String imageKey, String thumbnailKey) {
	}

}
