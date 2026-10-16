package com.urijip.server.location.controller;

import java.util.List;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.location.dto.request.LocationSharingRequest;
import com.urijip.server.location.dto.request.LocationUpdateRequest;
import com.urijip.server.location.dto.response.FamilyLocationResponse;
import com.urijip.server.location.dto.response.LocationSharingResponse;
import com.urijip.server.location.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LocationController {

	private final LocationService locationService;

	@GetMapping("/api/families/{familyId}/locations")
	public ApiResponse<List<FamilyLocationResponse>> getFamilyLocations(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId) {
		return ApiResponse.success(locationService.getFamilyLocations(familyId, userId));
	}

	@PutMapping("/api/users/me/location")
	public ApiResponse<Void> updateMyLocation(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody LocationUpdateRequest request) {
		locationService.updateMyLocation(userId, request);
		return ApiResponse.success(null);
	}

	@PatchMapping("/api/users/me/location-sharing")
	public ApiResponse<LocationSharingResponse> changeSharing(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody LocationSharingRequest request) {
		return ApiResponse.success(locationService.changeSharing(userId, request));
	}

}
