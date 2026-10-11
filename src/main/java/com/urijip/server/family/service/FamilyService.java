package com.urijip.server.family.service;

import java.util.Locale;

import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.entity.FamilyMember;
import com.urijip.server.family.entity.FamilyRole;
import com.urijip.server.family.repository.FamilyMemberRepository;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FamilyService {

	private final FamilyRepository familyRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final UserService userService;
	private final InviteCodeGenerator inviteCodeGenerator;

	@Transactional
	public FamilyResponse create(Long userId, FamilyCreateRequest request) {
		validateHasNoFamily(userId);
		User owner = userService.getUser(userId);

		Family family = familyRepository.save(Family.builder()
				.name(request.name())
				.inviteCode(newInviteCode())
				.owner(owner)
				.build());
		familyMemberRepository.save(FamilyMember.builder()
				.family(family)
				.user(owner)
				.role(FamilyRole.OWNER)
				.build());

		return FamilyResponse.from(family);
	}

	@Transactional
	public FamilyResponse join(Long userId, FamilyJoinRequest request) {
		Family family = familyRepository.findByInviteCode(normalize(request.inviteCode()))
				.orElseThrow(() -> new BusinessException(ErrorCode.INVITE_CODE_NOT_FOUND));
		User user = userService.getUser(userId);

		familyMemberRepository.save(FamilyMember.builder()
				.family(family)
				.user(user)
				.role(FamilyRole.MEMBER)
				.build());

		return FamilyResponse.from(family);
	}

	@Transactional(readOnly = true)
	public boolean hasFamily(Long userId) {
		return familyMemberRepository.existsByUserId(userId);
	}

	private String normalize(String inviteCode) {
		return inviteCode.trim().toUpperCase(Locale.ROOT);
	}

	private void validateHasNoFamily(Long userId) {
		if (familyMemberRepository.existsByUserId(userId)) {
			throw new BusinessException(ErrorCode.ALREADY_FAMILY_MEMBER);
		}
	}

	private String newInviteCode() {
		String code = inviteCodeGenerator.generate();
		while (familyRepository.existsByInviteCode(code)) {
			code = inviteCodeGenerator.generate();
		}
		return code;
	}

}
