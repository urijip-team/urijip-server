package com.urijip.server.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.photo.dto.response.PhotoCommentResponse;
import com.urijip.server.photo.entity.Album;
import com.urijip.server.photo.entity.Photo;
import com.urijip.server.photo.entity.PhotoComment;
import com.urijip.server.photo.repository.AlbumRepository;
import com.urijip.server.photo.repository.PhotoCommentRepository;
import com.urijip.server.photo.repository.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

// 댓글은 S3를 쓰지 않으므로 사진 행만 직접 만든다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PhotoCommentServiceTest {

	@Autowired
	private PhotoCommentService photoCommentService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private PhotoCommentRepository photoCommentRepository;

	@Autowired
	private PhotoRepository photoRepository;

	@Autowired
	private AlbumRepository albumRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User dad;

	private User stranger;

	private FamilyResponse family;

	private Photo photo;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		dad = saveUser("dad@example.com", "김아빠");
		stranger = saveUser("stranger@example.com", "남");
		family = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		familyService.join(dad.getId(), new FamilyJoinRequest(family.inviteCode()));
		photo = savePhoto(family.id());
	}

	@Test
	void getCommentsReturnsOldestFirstWithAuthor() {
		photoCommentRepository.save(new PhotoComment(photo, mom, "바다 좋다"));
		photoCommentRepository.save(new PhotoComment(photo, dad, "또 가자"));

		assertThat(photoCommentService.getComments(photo.getId(), dad.getId()))
				.extracting(PhotoCommentResponse::content, comment -> comment.author().id(),
						comment -> comment.author().name())
				.containsExactly(
						tuple("바다 좋다", mom.getId(), "김엄마"),
						tuple("또 가자", dad.getId(), "김아빠"));
	}

	@Test
	void getCommentsOfPhotoWithoutCommentsReturnsEmpty() {
		assertThat(photoCommentService.getComments(photo.getId(), mom.getId())).isEmpty();
	}

	@Test
	void getCommentsOfOtherFamilyPhotoIsForbidden() {
		assertError(() -> photoCommentService.getComments(photo.getId(), stranger.getId()),
				ErrorCode.FAMILY_NOT_MEMBER);
		assertError(() -> photoCommentService.getComments(999_999L, mom.getId()), ErrorCode.PHOTO_NOT_FOUND);
	}

	private Photo savePhoto(Long familyId) {
		Album album = albumRepository.save(Album.builder()
				.family(familyService.getFamily(familyId))
				.title("여름")
				.createdBy(mom)
				.build());
		return photoRepository.save(Photo.builder()
				.album(album)
				.uploader(mom)
				.imageKey("beach.jpg")
				.thumbnailKey("beach_thumb.jpg")
				.build());
	}

	private void assertError(Runnable action, ErrorCode errorCode) {
		assertThatThrownBy(action::run)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
