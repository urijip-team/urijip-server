package com.urijip.server.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.urijip.server.LocalStackTestConfiguration;
import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.photo.TestImages;
import com.urijip.server.photo.dto.request.AlbumCreateRequest;
import com.urijip.server.photo.dto.response.AlbumResponse;
import com.urijip.server.photo.dto.response.PhotoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import({TestcontainersConfiguration.class, LocalStackTestConfiguration.class})
@Transactional
class AlbumServiceTest {

	@Autowired
	private AlbumService albumService;

	@Autowired
	private PhotoService photoService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User dad;

	private FamilyResponse family;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		dad = saveUser("dad@example.com", "김아빠");
		family = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		familyService.join(dad.getId(), new FamilyJoinRequest(family.inviteCode()));
	}

	@Test
	void createReturnsEmptyAlbum() {
		AlbumResponse album = albumService.create(family.id(), mom.getId(), new AlbumCreateRequest("2026 여름휴가"));

		assertThat(album.id()).isNotNull();
		assertThat(album.title()).isEqualTo("2026 여름휴가");
		assertThat(album.coverUrl()).isNull();
		assertThat(album.photoCount()).isZero();
		assertThat(album.createdBy()).isEqualTo(mom.getId());
	}

	@Test
	void getAlbumsReturnsNewestFirstWithCoverAndPhotoCount() {
		AlbumResponse summer = albumService.create(family.id(), mom.getId(), new AlbumCreateRequest("여름휴가"));
		AlbumResponse birthday = albumService.create(family.id(), dad.getId(), new AlbumCreateRequest("생일"));
		List<PhotoResponse> photos = photoService.upload(summer.id(), dad.getId(),
				List.of(TestImages.jpeg(800, 600), TestImages.jpeg(600, 800)));

		List<AlbumResponse> albums = albumService.getAlbums(family.id(), dad.getId());

		assertThat(albums).extracting(AlbumResponse::id).containsExactly(birthday.id(), summer.id());
		assertThat(albums).extracting(AlbumResponse::photoCount).containsExactly(0L, 2L);
		assertThat(albums.get(0).coverUrl()).isNull();
		assertThat(albums.get(1).coverUrl()).contains(thumbnailKeyOf(photos.getFirst()));
	}

	@Test
	void getAlbumsOfOtherFamilyIsForbidden() {
		User stranger = saveUser("stranger@example.com", "남");

		assertThatThrownBy(() -> albumService.getAlbums(family.id(), stranger.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
		assertThatThrownBy(() -> albumService.create(family.id(), stranger.getId(), new AlbumCreateRequest("몰래")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void getAccessibleAlbumChecksExistenceAndMembership() {
		AlbumResponse album = albumService.create(family.id(), mom.getId(), new AlbumCreateRequest("여름휴가"));
		User stranger = saveUser("stranger@example.com", "남");

		assertThatThrownBy(() -> albumService.getAccessibleAlbum(999_999L, mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ALBUM_NOT_FOUND);
		assertThatThrownBy(() -> albumService.getAccessibleAlbum(album.id(), stranger.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	// 서명 URL의 경로에서 썸네일 key 부분을 뽑는다.
	private String thumbnailKeyOf(PhotoResponse photo) {
		String path = java.net.URI.create(photo.thumbnailUrl()).getPath();
		return path.substring(path.indexOf("families/"));
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
