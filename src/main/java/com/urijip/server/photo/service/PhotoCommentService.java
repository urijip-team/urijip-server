package com.urijip.server.photo.service;

import java.util.List;

import com.urijip.server.photo.dto.response.PhotoCommentResponse;
import com.urijip.server.photo.repository.PhotoCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhotoCommentService {

	private final PhotoCommentRepository photoCommentRepository;
	private final PhotoService photoService;

	@Transactional(readOnly = true)
	public List<PhotoCommentResponse> getComments(Long photoId, Long userId) {
		photoService.getAccessiblePhoto(photoId, userId);

		return photoCommentRepository.findByPhotoIdOrderByIdAsc(photoId).stream()
				.map(PhotoCommentResponse::from)
				.toList();
	}

}
