package com.urijip.server.family.service;

import java.util.Map;
import java.util.stream.Collectors;

import com.urijip.server.family.dto.request.AdminFamilyListRequest;
import com.urijip.server.family.dto.request.AdminFamilyStatusRequest;
import com.urijip.server.family.dto.response.AdminFamilyResponse;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.repository.FamilyMemberCount;
import com.urijip.server.family.repository.FamilyMemberRepository;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminFamilyService {

	private final FamilyRepository familyRepository;
	private final FamilyMemberRepository familyMemberRepository;

	@Transactional(readOnly = true)
	public PageResponse<AdminFamilyResponse> getFamilies(AdminFamilyListRequest request) {
		Page<Family> families = familyRepository.findAll(PageRequest.of(request.pageOrDefault(),
				request.sizeOrDefault(), Sort.by(Sort.Direction.DESC, "id")));

		Map<Long, Long> memberCounts = familyMemberRepository
				.countByFamilyIds(families.map(Family::getId).getContent())
				.stream()
				.collect(Collectors.toMap(FamilyMemberCount::getFamilyId, FamilyMemberCount::getMemberCount));

		return PageResponse.from(families.map(family ->
				AdminFamilyResponse.of(family, memberCounts.getOrDefault(family.getId(), 0L))));
	}

	// 정지된 가족은 초대 코드로 참여할 수 없다(FamilyService.join).
	@Transactional
	public AdminFamilyResponse changeStatus(Long familyId, AdminFamilyStatusRequest request) {
		Family family = familyRepository.findById(familyId)
				.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		family.changeStatus(request.status());

		return AdminFamilyResponse.of(family, familyMemberRepository.countByFamilyId(familyId));
	}

}
