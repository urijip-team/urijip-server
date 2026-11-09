package com.urijip.server.place.controller;

import java.util.List;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.place.dto.request.PlaceCreateRequest;
import com.urijip.server.place.dto.request.PlaceUpdateRequest;
import com.urijip.server.place.dto.response.PlaceResponse;
import com.urijip.server.place.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlaceController {

	private final PlaceService placeService;

	@GetMapping("/api/families/{familyId}/places")
	public ApiResponse<List<PlaceResponse>> getPlaces(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId) {
		return ApiResponse.success(placeService.getPlaces(familyId, userId));
	}

	@PostMapping("/api/families/{familyId}/places")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<PlaceResponse> create(@AuthenticationPrincipal Long userId, @PathVariable Long familyId,
			@Valid @RequestBody PlaceCreateRequest request) {
		return ApiResponse.success(placeService.create(familyId, userId, request));
	}

	@PatchMapping("/api/places/{placeId}")
	public ApiResponse<PlaceResponse> update(@AuthenticationPrincipal Long userId, @PathVariable Long placeId,
			@Valid @RequestBody PlaceUpdateRequest request) {
		return ApiResponse.success(placeService.update(placeId, userId, request));
	}

	@DeleteMapping("/api/places/{placeId}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long placeId) {
		placeService.delete(placeId, userId);
		return ApiResponse.success(null);
	}

}
