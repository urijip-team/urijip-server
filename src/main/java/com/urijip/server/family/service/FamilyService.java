package com.urijip.server.family.service;

import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.entity.FamilyMember;
import com.urijip.server.family.entity.FamilyRole;
import com.urijip.server.family.repository.FamilyMemberRepository;
import com.urijip.server.family.repository.FamilyRepository;
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
		User owner = userService.getUser(userId);

		Family family = familyRepository.save(Family.builder()
				.name(request.name())
				.inviteCode(inviteCodeGenerator.generate())
				.owner(owner)
				.build());
		familyMemberRepository.save(FamilyMember.builder()
				.family(family)
				.user(owner)
				.role(FamilyRole.OWNER)
				.build());

		return FamilyResponse.from(family);
	}

}
