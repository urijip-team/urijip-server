package com.urijip.server.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import com.urijip.server.LocalStackTestConfiguration;
import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.photo.TestImages;
import com.urijip.server.photo.dto.request.AlbumCreateRequest;
import com.urijip.server.photo.dto.request.PhotoPageRequest;
import com.urijip.server.photo.dto.response.AlbumResponse;
import com.urijip.server.photo.dto.response.PhotoResponse;
import com.urijip.server.photo.repository.AlbumRepository;
import com.urijip.server.photo.repository.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

@SpringBootTest
@Import({TestcontainersConfiguration.class, LocalStackTestConfiguration.class})
@Transactional
@RecordApplicationEvents
class PhotoServiceTest {

	private static final String BUCKET = "urijip-local";

	@Autowired
	private PhotoService photoService;

	@Autowired
	private AlbumService albumService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PhotoRepository photoRepository;

	@Autowired
	private AlbumRepository albumRepository;

	@Autowired
	private S3Client s3Client;

	@Autowired
	private ApplicationEvents events;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private User mom;

	private User dad;

	private FamilyResponse family;

	private AlbumResponse album;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		dad = saveUser("dad@example.com", "김아빠");
		family = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		familyService.join(dad.getId(), new FamilyJoinRequest(family.inviteCode()));
		album = albumService.create(family.id(), mom.getId(), new AlbumCreateRequest("여름휴가"));
	}

	@Test
	void uploadStoresOriginalAndThumbnailInS3() throws Exception {
		MockMultipartFile file = TestImages.jpeg(1200, 800);

		List<PhotoResponse> photos = photoService.upload(album.id(), dad.getId(), List.of(file));

		PhotoResponse photo = photos.getFirst();
		assertThat(photo.id()).isNotNull();
		assertThat(photo.uploader()).isEqualTo(new PhotoResponse.Uploader(dad.getId(), "김아빠"));
		assertThat(photo.createdAt()).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));
		assertThat(download(photo.imageUrl())).isEqualTo(file.getBytes());
		assertThat(download(photo.thumbnailUrl())).isNotEmpty().isNotEqualTo(file.getBytes());
		assertThat(objectKeys()).hasSize(2)
				.allMatch(key -> key.startsWith("families/%d/albums/%d/".formatted(family.id(), album.id())));
	}

	@Test
	void firstUploadedPhotoBecomesCover() {
		List<PhotoResponse> first = photoService.upload(album.id(), mom.getId(),
				List.of(TestImages.jpeg(10, 10), TestImages.jpeg(20, 20)));
		photoService.upload(album.id(), mom.getId(), List.of(TestImages.jpeg(30, 30)));

		assertThat(albumRepository.findById(album.id()).orElseThrow().getCoverPhoto().getId())
				.isEqualTo(first.getFirst().id());
	}

	@Test
	void uploadRejectsTooManyOrNoFiles() {
		List<MultipartFile> eleven = Collections.nCopies(11, TestImages.jpeg(10, 10));

		assertError(() -> photoService.upload(album.id(), mom.getId(), eleven), ErrorCode.TOO_MANY_PHOTOS);
		assertError(() -> photoService.upload(album.id(), mom.getId(), List.of()), ErrorCode.INVALID_INPUT);
		assertThat(objectKeys()).isEmpty();
	}

	@Test
	void uploadWithInvalidFileRemovesAlreadyUploadedFiles() {
		List<MultipartFile> files = List.of(TestImages.jpeg(10, 10), TestImages.file("not an image".getBytes()));

		assertError(() -> photoService.upload(album.id(), mom.getId(), files), ErrorCode.INVALID_IMAGE);
		assertThat(objectKeys()).isEmpty();
		assertThat(photoRepository.count()).isZero();
	}

	@Test
	void uploadToOtherFamilyAlbumIsForbidden() {
		User stranger = saveUser("stranger@example.com", "남");

		assertError(() -> photoService.upload(album.id(), stranger.getId(), List.of(TestImages.jpeg(10, 10))),
				ErrorCode.FAMILY_NOT_MEMBER);
		assertError(() -> photoService.upload(999_999L, mom.getId(), List.of(TestImages.jpeg(10, 10))),
				ErrorCode.ALBUM_NOT_FOUND);
		assertThat(objectKeys()).isEmpty();
	}

	@Test
	void getPhotosReturnsNewestFirstWithPaging() {
		List<PhotoResponse> uploaded = photoService.upload(album.id(), mom.getId(),
				List.of(TestImages.jpeg(10, 10), TestImages.jpeg(10, 10), TestImages.jpeg(10, 10)));

		PageResponse<PhotoResponse> firstPage = photoService.getPhotos(album.id(), dad.getId(),
				new PhotoPageRequest(0, 2));
		PageResponse<PhotoResponse> secondPage = photoService.getPhotos(album.id(), dad.getId(),
				new PhotoPageRequest(1, 2));

		assertThat(firstPage.content()).extracting(PhotoResponse::id)
				.containsExactly(uploaded.get(2).id(), uploaded.get(1).id());
		assertThat(secondPage.content()).extracting(PhotoResponse::id).containsExactly(uploaded.get(0).id());
		assertThat(firstPage.totalElements()).isEqualTo(3);
		assertThat(firstPage.content().getFirst().thumbnailUrl()).isNotBlank();
	}

	@Test
	void getPhotosUsesDefaultPaging() {
		PageResponse<PhotoResponse> page = photoService.getPhotos(album.id(), mom.getId(),
				new PhotoPageRequest(null, null));

		assertThat(page.page()).isZero();
		assertThat(page.size()).isEqualTo(30);
	}

	@Test
	void deleteByUploaderRemovesRowAndRequestsFileDeletion() {
		PhotoResponse photo = photoService.upload(album.id(), dad.getId(), List.of(TestImages.jpeg(10, 10)))
				.getFirst();

		photoService.delete(photo.id(), dad.getId());

		assertThat(photoRepository.findById(photo.id())).isEmpty();
		// 파일은 트랜잭션이 커밋된 뒤에 지운다. 테스트 트랜잭션은 롤백되므로 아직 남아 있다.
		List<String> keys = objectKeys();
		assertThat(keys).hasSize(2);
		String imageKey = keys.stream().filter(key -> !key.contains("_thumb")).findFirst().orElseThrow();
		String thumbnailKey = keys.stream().filter(key -> key.contains("_thumb")).findFirst().orElseThrow();
		assertThat(events.stream(PhotoFilesDeletedEvent.class))
				.singleElement()
				.isEqualTo(new PhotoFilesDeletedEvent(List.of(imageKey, thumbnailKey)));
	}

	@Test
	void deletingCoverPicksNewestRemainingPhoto() {
		List<PhotoResponse> photos = photoService.upload(album.id(), mom.getId(),
				List.of(TestImages.jpeg(10, 10), TestImages.jpeg(10, 10), TestImages.jpeg(10, 10)));

		photoService.delete(photos.get(0).id(), mom.getId());
		assertThat(albumRepository.findById(album.id()).orElseThrow().getCoverPhoto().getId())
				.isEqualTo(photos.get(2).id());

		photoService.delete(photos.get(2).id(), mom.getId());
		photoService.delete(photos.get(1).id(), mom.getId());
		assertThat(albumRepository.findById(album.id()).orElseThrow().getCoverPhoto()).isNull();
	}

	@Test
	void onlyUploaderCanDelete() {
		PhotoResponse photo = photoService.upload(album.id(), dad.getId(), List.of(TestImages.jpeg(10, 10)))
				.getFirst();
		User stranger = saveUser("stranger@example.com", "남");

		assertError(() -> photoService.delete(photo.id(), mom.getId()), ErrorCode.NOT_PHOTO_UPLOADER);
		assertError(() -> photoService.delete(photo.id(), stranger.getId()), ErrorCode.FAMILY_NOT_MEMBER);
		assertError(() -> photoService.delete(999_999L, dad.getId()), ErrorCode.PHOTO_NOT_FOUND);
		assertThat(photoRepository.findById(photo.id())).isPresent();
	}

	@Test
	void getAccessiblePhotoChecksFamilyMembership() {
		PhotoResponse photo = photoService.upload(album.id(), dad.getId(), List.of(TestImages.jpeg(10, 10)))
				.getFirst();
		User stranger = saveUser("stranger@example.com", "남");

		assertThat(photoService.getAccessiblePhoto(photo.id(), mom.getId()).getId()).isEqualTo(photo.id());
		assertError(() -> photoService.getAccessiblePhoto(photo.id(), stranger.getId()),
				ErrorCode.FAMILY_NOT_MEMBER);
		assertError(() -> photoService.getAccessiblePhoto(999_999L, mom.getId()), ErrorCode.PHOTO_NOT_FOUND);
	}

	@Test
	void deletingFamilyRemovesAlbumsAndPhotosEvenWithCover() {
		photoService.upload(album.id(), mom.getId(), List.of(TestImages.jpeg(10, 10), TestImages.jpeg(10, 10)));
		albumService.create(family.id(), mom.getId(), new AlbumCreateRequest("빈 앨범"));
		familyService.removeMember(family.id(), dad.getId(), dad.getId());

		familyService.removeMember(family.id(), mom.getId(), mom.getId());

		assertThat(albumRepository.count()).isZero();
		assertThat(photoRepository.count()).isZero();
		assertThat(events.stream(PhotoFilesDeletedEvent.class))
				.singleElement()
				.extracting(event -> event.keys().size())
				.isEqualTo(4);
	}

	private List<String> objectKeys() {
		return s3Client.listObjectsV2(request -> request.bucket(BUCKET)
						.prefix("families/%d/".formatted(family.id())))
				.contents().stream()
				.map(S3Object::key)
				.toList();
	}

	private byte[] download(String url) throws IOException, InterruptedException {
		HttpResponse<byte[]> response = httpClient.send(HttpRequest.newBuilder(URI.create(url)).build(),
				HttpResponse.BodyHandlers.ofByteArray());
		assertThat(response.statusCode()).isEqualTo(200);
		return response.body();
	}

	private void assertError(Runnable action, ErrorCode errorCode) {
		assertThatThrownBy(action::run)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
