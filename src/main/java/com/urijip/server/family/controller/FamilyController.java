package com.urijip.server.family.controller;

import java.util.List;

import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.request.FamilyOwnerTransferRequest;
import com.urijip.server.family.dto.request.NicknameUpdateRequest;
import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.response.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class FamilyController {

	private final FamilyService familyService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<FamilyResponse> create(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody FamilyCreateRequest request) {
		return ApiResponse.success(familyService.create(userId, request));
	}

	@GetMapping("/me")
	public ApiResponse<FamilyResponse> getMyFamily(@AuthenticationPrincipal Long userId) {
		return ApiResponse.success(familyService.getMyFamily(userId));
	}

	@GetMapping("/{familyId}/members")
	public ApiResponse<List<FamilyMemberResponse>> getMembers(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId) {
		return ApiResponse.success(familyService.getMembers(familyId, userId));
	}

	@PostMapping("/join")
	public ApiResponse<FamilyResponse> join(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody FamilyJoinRequest request) {
		return ApiResponse.success(familyService.join(userId, request));
	}

	@PostMapping("/{familyId}/invite-code")
	public ApiResponse<FamilyResponse> reissueInviteCode(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId) {
		return ApiResponse.success(familyService.reissueInviteCode(familyId, userId));
	}

	@PatchMapping("/{familyId}/owner")
	public ApiResponse<List<FamilyMemberResponse>> transferOwner(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId, @Valid @RequestBody FamilyOwnerTransferRequest request) {
		return ApiResponse.success(familyService.transferOwner(familyId, userId, request));
	}

	@PatchMapping("/{familyId}/members/me")
	public ApiResponse<FamilyMemberResponse> changeMyNickname(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId, @Valid @RequestBody NicknameUpdateRequest request) {
		return ApiResponse.success(familyService.changeMyNickname(familyId, userId, request));
	}

	@DeleteMapping("/{familyId}/members/{userId}")
	public ApiResponse<Void> removeMember(@AuthenticationPrincipal Long userId, @PathVariable Long familyId,
			@PathVariable("userId") Long targetUserId) {
		familyService.removeMember(familyId, userId, targetUserId);
		return ApiResponse.success(null);
	}

}
