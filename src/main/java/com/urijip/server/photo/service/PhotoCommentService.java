package com.urijip.server.photo.service;

import java.util.List;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.service.UserService;
import com.urijip.server.photo.dto.request.PhotoCommentCreateRequest;
import com.urijip.server.photo.dto.response.PhotoCommentResponse;
import com.urijip.server.photo.entity.Photo;
import com.urijip.server.photo.entity.PhotoComment;
import com.urijip.server.photo.repository.PhotoCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhotoCommentService {

	private final PhotoCommentRepository photoCommentRepository;
	private final PhotoService photoService;
	private final AlbumService albumService;
	private final UserService userService;

	@Transactional(readOnly = true)
	public List<PhotoCommentResponse> getComments(Long photoId, Long userId) {
		photoService.getAccessiblePhoto(photoId, userId);

		return photoCommentRepository.findByPhotoIdOrderByIdAsc(photoId).stream()
				.map(PhotoCommentResponse::from)
				.toList();
	}

	@Transactional
	public PhotoCommentResponse create(Long photoId, Long userId, PhotoCommentCreateRequest request) {
		Photo photo = photoService.getAccessiblePhoto(photoId, userId);

		PhotoComment comment = photoCommentRepository.save(
				new PhotoComment(photo, userService.getUser(userId), request.content()));

		return PhotoCommentResponse.from(comment);
	}

	// 쓴 사람만 지울 수 있다. 가족을 나간 뒤에는 그 가족의 댓글을 지울 수 없다.
	@Transactional
	public void delete(Long commentId, Long userId) {
		PhotoComment comment = photoCommentRepository.findWithPhotoById(commentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
		albumService.getAccessibleAlbum(comment.getPhoto().getAlbum().getId(), userId);
		if (!comment.getUser().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.NOT_COMMENT_AUTHOR);
		}

		photoCommentRepository.delete(comment);
	}

}
