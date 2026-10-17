package com.urijip.server.photo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.photo.entity.Album;
import com.urijip.server.photo.entity.Photo;
import com.urijip.server.photo.entity.PhotoComment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class PhotoCommentRepositoryTest {

	@Autowired
	private PhotoCommentRepository photoCommentRepository;

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

	private Photo beach;

	private Photo mountain;

	@BeforeEach
	void setUp() {
		mom = userRepository.saveAndFlush(User.builder()
				.email("mom@example.com")
				.password("encoded-password")
				.name("김엄마")
				.build());
		kims = familyRepository.saveAndFlush(Family.builder().name("김씨네").inviteCode("A3K9P2").owner(mom).build());
		Album album = albumRepository.saveAndFlush(Album.builder().family(kims).title("여름").createdBy(mom).build());
		beach = photo(album, "beach");
		mountain = photo(album, "mountain");
	}

	@Test
	void savedCommentGetsCreatedAt() {
		PhotoComment comment = photoCommentRepository.saveAndFlush(new PhotoComment(beach, mom, "바다 좋다"));

		assertThat(comment.getId()).isNotNull();
		assertThat(comment.getCreatedAt()).isNotNull();
	}

	@Test
	void findByPhotoIdReturnsOldestFirstOnlyForThatPhoto() {
		photoCommentRepository.saveAndFlush(new PhotoComment(beach, mom, "첫 댓글"));
		photoCommentRepository.saveAndFlush(new PhotoComment(mountain, mom, "다른 사진"));
		photoCommentRepository.saveAndFlush(new PhotoComment(beach, mom, "두 번째"));

		assertThat(photoCommentRepository.findByPhotoIdOrderByIdAsc(beach.getId()))
				.extracting(PhotoComment::getContent)
				.containsExactly("첫 댓글", "두 번째");
	}

	@Test
	void deleteByPhotoIdDeletesOnlyThatPhotoComments() {
		photoCommentRepository.saveAndFlush(new PhotoComment(beach, mom, "1"));
		photoCommentRepository.saveAndFlush(new PhotoComment(beach, mom, "2"));
		photoCommentRepository.saveAndFlush(new PhotoComment(mountain, mom, "남음"));

		assertThat(photoCommentRepository.deleteByPhotoId(beach.getId())).isEqualTo(2);
		assertThat(photoCommentRepository.findAll()).extracting(PhotoComment::getContent).containsExactly("남음");
	}

	private Photo photo(Album album, String name) {
		return photoRepository.saveAndFlush(Photo.builder()
				.album(album)
				.uploader(mom)
				.imageKey(name + ".jpg")
				.thumbnailKey(name + "_thumb.jpg")
				.build());
	}

}
