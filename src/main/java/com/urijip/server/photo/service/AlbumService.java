package com.urijip.server.photo.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.storage.FileStorage;
import com.urijip.server.member.service.UserService;
import com.urijip.server.photo.dto.request.AlbumCreateRequest;
import com.urijip.server.photo.dto.response.AlbumResponse;
import com.urijip.server.photo.entity.Album;
import com.urijip.server.photo.entity.Photo;
import com.urijip.server.photo.repository.AlbumPhotoCount;
import com.urijip.server.photo.repository.AlbumRepository;
import com.urijip.server.photo.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlbumService {

	private final AlbumRepository albumRepository;
	private final PhotoRepository photoRepository;
	private final FamilyService familyService;
	private final UserService userService;
	private final FileStorage fileStorage;

	@Transactional
	public AlbumResponse create(Long familyId, Long userId, AlbumCreateRequest request) {
		familyService.validateMember(familyId, userId);

		Album album = albumRepository.save(Album.builder()
				.family(familyService.getFamily(familyId))
				.title(request.title())
				.createdBy(userService.getUser(userId))
				.build());

		return AlbumResponse.of(album, null, 0);
	}

	// 최신순. 앨범마다 대표 사진 썸네일과 사진 수를 함께 준다.
	@Transactional(readOnly = true)
	public List<AlbumResponse> getAlbums(Long familyId, Long userId) {
		familyService.validateMember(familyId, userId);

		List<Album> albums = albumRepository.findByFamilyIdOrderByIdDesc(familyId);
		Map<Long, Long> photoCounts = photoRepository.countByAlbumIds(albums.stream().map(Album::getId).toList())
				.stream()
				.collect(Collectors.toMap(AlbumPhotoCount::getAlbumId, AlbumPhotoCount::getPhotoCount));

		return albums.stream()
				.map(album -> AlbumResponse.of(album, coverUrl(album), photoCounts.getOrDefault(album.getId(), 0L)))
				.toList();
	}

	// 앨범이 없으면 404, 요청자가 그 가족의 멤버가 아니면 403.
	@Transactional(readOnly = true)
	public Album getAccessibleAlbum(Long albumId, Long userId) {
		Album album = albumRepository.findById(albumId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));
		familyService.validateMember(album.getFamily().getId(), userId);
		return album;
	}

	private String coverUrl(Album album) {
		Photo cover = album.getCoverPhoto();
		return cover == null ? null : fileStorage.createUrl(cover.getThumbnailKey());
	}

}
