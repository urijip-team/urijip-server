package com.urijip.server.photo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.photo.entity.Album;
import com.urijip.server.photo.entity.Photo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class PhotoRepositoryTest {

	@Autowired
	private PhotoRepository photoRepository;

	@Autowired
	private AlbumRepository albumRepository;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private Family kims;

	private Family parks;

	@BeforeEach
	void setUp() {
		mom = userRepository.saveAndFlush(User.builder()
				.email("mom@example.com")
				.password("encoded-password")
				.name("김엄마")
				.build());
		kims = familyRepository.saveAndFlush(Family.builder().name("김씨네").inviteCode("A3K9P2").owner(mom).build());
		parks = familyRepository.saveAndFlush(Family.builder().name("박씨네").inviteCode("B7Q4M1").owner(mom).build());
	}

	@Test
	void savedPhotoKeepsKeysInUrlColumnsAndGetsCreatedAt() {
		Photo photo = photoRepository.saveAndFlush(photo(album(kims, "여름"), "a"));

		assertThat(photo.getImageKey()).isEqualTo("a.jpg");
		assertThat(photo.getThumbnailKey()).isEqualTo("a_thumb.jpg");
		assertThat(photo.getCreatedAt()).isNotNull();
	}

	@Test
	void countByAlbumIdsCountsPhotosPerAlbum() {
		Album summer = album(kims, "여름");
		Album winter = album(kims, "겨울");
		Album empty = album(kims, "빈 앨범");
		photoRepository.saveAllAndFlush(List.of(photo(summer, "a"), photo(summer, "b"), photo(winter, "c")));

		assertThat(photoRepository.countByAlbumIds(List.of(summer.getId(), winter.getId(), empty.getId())))
				.extracting(AlbumPhotoCount::getAlbumId, AlbumPhotoCount::getPhotoCount)
				.containsExactlyInAnyOrder(tuple(summer.getId(), 2L), tuple(winter.getId(), 1L));
		assertThat(photoRepository.countByAlbumIds(List.of())).isEmpty();
	}

	@Test
	void findsPhotosAndAlbumsOfFamily() {
		Album summer = album(kims, "여름");
		Photo a = photoRepository.saveAndFlush(photo(summer, "a"));
		Photo b = photoRepository.saveAndFlush(photo(summer, "b"));
		Album other = album(parks, "다른 가족");
		photoRepository.saveAndFlush(photo(other, "c"));

		assertThat(albumRepository.findByFamilyId(kims.getId())).extracting(Album::getId)
				.containsExactly(summer.getId());
		assertThat(photoRepository.findByAlbumFamilyId(kims.getId())).extracting(Photo::getId)
				.containsExactlyInAnyOrder(a.getId(), b.getId());
	}

	private Album album(Family family, String title) {
		return albumRepository.saveAndFlush(Album.builder().family(family).title(title).createdBy(mom).build());
	}

	private Photo photo(Album album, String name) {
		return Photo.builder()
				.album(album)
				.uploader(mom)
				.imageKey(name + ".jpg")
				.thumbnailKey(name + "_thumb.jpg")
				.build();
	}

}
