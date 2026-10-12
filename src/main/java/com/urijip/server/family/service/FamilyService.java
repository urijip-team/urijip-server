package com.urijip.server.family.service;

import java.util.List;
import java.util.Locale;

import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.request.FamilyOwnerTransferRequest;
import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.entity.FamilyMember;
import com.urijip.server.family.entity.FamilyRole;
import com.urijip.server.family.entity.FamilyStatus;
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
		validateHasNoFamily(userId);
		Family family = familyRepository.findByInviteCode(normalize(request.inviteCode()))
				.filter(found -> found.getStatus() == FamilyStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVITE_CODE_NOT_FOUND));
		User user = userService.getUser(userId);

		familyMemberRepository.save(FamilyMember.builder()
				.family(family)
				.user(user)
				.role(FamilyRole.MEMBER)
				.build());

		return FamilyResponse.from(family);
	}

	@Transactional
	public FamilyResponse reissueInviteCode(Long familyId, Long userId) {
		validateOwner(familyId, userId);
		Family family = familyRepository.findById(familyId)
				.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		family.changeInviteCode(newInviteCode());

		return FamilyResponse.from(family);
	}

	@Transactional
	public List<FamilyMemberResponse> transferOwner(Long familyId, Long userId,
			FamilyOwnerTransferRequest request) {
		validateOwner(familyId, userId);
		if (userId.equals(request.userId())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		FamilyMember newOwner = getMember(familyId, request.userId());
		FamilyMember oldOwner = getMember(familyId, userId);

		oldOwner.changeRole(FamilyRole.MEMBER);
		newOwner.changeRole(FamilyRole.OWNER);
		newOwner.getFamily().changeOwner(newOwner.getUser());

		return getMembers(familyId, userId);
	}

	@Transactional
	public void removeMember(Long familyId, Long userId, Long targetUserId) {
		if (userId.equals(targetUserId)) {
			leave(familyId, userId);
			return;
		}
		validateOwner(familyId, userId);
		familyMemberRepository.delete(getMember(familyId, targetUserId));
	}

	@Transactional(readOnly = true)
	public FamilyResponse getMyFamily(Long userId) {
		return familyMemberRepository.findFirstByUserId(userId)
				.map(member -> FamilyResponse.from(member.getFamily()))
				.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public List<FamilyMemberResponse> getMembers(Long familyId, Long userId) {
		validateMember(familyId, userId);

		return familyMemberRepository.findByFamilyIdOrderByJoinedAtAscIdAsc(familyId).stream()
				.map(FamilyMemberResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public void validateMember(Long familyId, Long userId) {
		if (!familyMemberRepository.existsByFamilyIdAndUserId(familyId, userId)) {
			throw new BusinessException(ErrorCode.FAMILY_NOT_MEMBER);
		}
	}

	@Transactional(readOnly = true)
	public void validateOwner(Long familyId, Long userId) {
		validateMember(familyId, userId);
		if (!familyMemberRepository.existsByFamilyIdAndUserIdAndRole(familyId, userId, FamilyRole.OWNER)) {
			throw new BusinessException(ErrorCode.NOT_FAMILY_OWNER);
		}
	}

	@Transactional(readOnly = true)
	public boolean hasFamily(Long userId) {
		return familyMemberRepository.existsByUserId(userId);
	}

	private void leave(Long familyId, Long userId) {
		validateMember(familyId, userId);
		FamilyMember member = getMember(familyId, userId);
		boolean owner = member.getRole() == FamilyRole.OWNER;
		if (owner && familyMemberRepository.countByFamilyId(familyId) > 1) {
			throw new BusinessException(ErrorCode.OWNER_CANNOT_LEAVE);
		}

		familyMemberRepository.delete(member);
	}

	private FamilyMember getMember(Long familyId, Long userId) {
		return familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_MEMBER_NOT_FOUND));
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
