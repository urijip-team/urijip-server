package com.urijip.server.family.service;

import com.urijip.server.family.dto.request.FamilyCreateRequest;
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
		if (familyMemberRepository.existsByUserId(userId)) {
			throw new BusinessException(ErrorCode.ALREADY_FAMILY_MEMBER);
		}
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

	private String newInviteCode() {
		String code = inviteCodeGenerator.generate();
		while (familyRepository.existsByInviteCode(code)) {
			code = inviteCodeGenerator.generate();
		}
		return code;
	}

}
