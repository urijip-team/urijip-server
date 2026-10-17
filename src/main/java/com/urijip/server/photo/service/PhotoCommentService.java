package com.urijip.server.photo.service;

import java.util.List;

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

}
