package com.urijip.server.family.controller;

import com.urijip.server.family.dto.request.AdminFamilyListRequest;
import com.urijip.server.family.dto.request.AdminFamilyStatusRequest;
import com.urijip.server.family.dto.response.AdminFamilyResponse;
import com.urijip.server.family.service.AdminFamilyService;
import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/families")
@RequiredArgsConstructor
public class AdminFamilyController {

	private final AdminFamilyService adminFamilyService;

	@GetMapping
	public ApiResponse<PageResponse<AdminFamilyResponse>> getFamilies(@ModelAttribute AdminFamilyListRequest request) {
		return ApiResponse.success(adminFamilyService.getFamilies(request));
	}

	@PatchMapping("/{familyId}/status")
	public ApiResponse<AdminFamilyResponse> changeStatus(@PathVariable Long familyId,
			@Valid @RequestBody AdminFamilyStatusRequest request) {
		return ApiResponse.success(adminFamilyService.changeStatus(familyId, request));
	}

}
