package com.urijip.server.family.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.request.FamilyOwnerTransferRequest;
import com.urijip.server.family.dto.request.NicknameUpdateRequest;
import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.dto.response.MemberReadState;
import com.urijip.server.family.dto.response.ReadProgress;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.entity.FamilyMember;
import com.urijip.server.family.entity.FamilyRole;
import com.urijip.server.family.entity.FamilyStatus;
import com.urijip.server.family.repository.FamilyMemberRepository;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.FamilyMembershipChecker;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FamilyService implements FamilyMembershipChecker {

	private final FamilyRepository familyRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final UserService userService;
	private final InviteCodeGenerator inviteCodeGenerator;
	private final ApplicationEventPublisher eventPublisher;

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
		Family family = getFamily(familyId);

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

	@Transactional
	public FamilyMemberResponse changeMyNickname(Long familyId, Long userId, NicknameUpdateRequest request) {
		validateMember(familyId, userId);
		FamilyMember member = getMember(familyId, userId);

		member.changeNickname(request.nickname());

		return FamilyMemberResponse.from(member);
	}

	@Transactional
	public void leaveFamily(Long userId) {
		familyMemberRepository.findFirstByUserId(userId)
				.ifPresent(member -> leave(member.getFamily().getId(), userId));
	}

	@Transactional(readOnly = true)
	public FamilyResponse getMyFamily(Long userId) {
		return findFamilyOf(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public Optional<FamilyResponse> findFamilyOf(Long userId) {
		return familyMemberRepository.findFirstByUserId(userId)
				.map(member -> FamilyResponse.from(member.getFamily()));
	}

	@Transactional(readOnly = true)
	public List<FamilyMemberResponse> getMembers(Long familyId, Long userId) {
		validateMember(familyId, userId);

		return familyMemberRepository.findByFamilyIdOrderByJoinedAtAscIdAsc(familyId).stream()
				.map(FamilyMemberResponse::from)
				.toList();
	}

	// 멤버가 아니면 FAMILY_NOT_MEMBER. 이미 그 메시지 이후까지 읽었으면 바뀐 것이 없어 비어 있다.
	@Transactional
	public Optional<ReadProgress> markRead(Long familyId, Long userId, Long messageId) {
		validateMember(familyId, userId);
		FamilyMember member = getMember(familyId, userId);
		Long previous = member.getLastReadMessageId();
		if (!member.markRead(messageId)) {
			return Optional.empty();
		}
		return Optional.of(new ReadProgress(previous, messageId));
	}

	// 멤버인지 검사하지 않는다. 채팅 서비스가 이미 검사한 뒤 부른다.
	@Transactional(readOnly = true)
	public List<MemberReadState> getReadStates(Long familyId) {
		return familyMemberRepository.findByFamilyIdOrderByJoinedAtAscIdAsc(familyId).stream()
				.map(MemberReadState::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public Family getFamily(Long familyId) {
		return familyRepository.findById(familyId)
				.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public List<User> getMemberUsers(Long familyId) {
		return familyMemberRepository.findByFamilyIdOrderByJoinedAtAscIdAsc(familyId).stream()
				.map(FamilyMember::getUser)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public void validateMember(Long familyId, Long userId) {
		if (!familyMemberRepository.existsByFamilyIdAndUserId(familyId, userId)) {
			throw new BusinessException(ErrorCode.FAMILY_NOT_MEMBER);
		}
	}

	// 멤버가 아니면 FAMILY_NOT_MEMBER, 관리자가 정지한 가족이면 FAMILY_SUSPENDED.
	// 정지된 가족에서도 SOS·위치 공유·가족 나가기는 되므로 그 기능은 validateMember를 쓴다.
	@Transactional(readOnly = true)
	public void validateActiveMember(Long familyId, Long userId) {
		validateMember(familyId, userId);
		validateActive(familyId);
	}

	// 관리자가 정지한 가족이면 FAMILY_SUSPENDED.
	@Transactional(readOnly = true)
	public void validateActive(Long familyId) {
		if (getFamily(familyId).isSuspended()) {
			throw new BusinessException(ErrorCode.FAMILY_SUSPENDED);
		}
	}

	@Transactional(readOnly = true)
	public void validateOwner(Long familyId, Long userId) {
		validateMember(familyId, userId);
		if (!familyMemberRepository.existsByFamilyIdAndUserIdAndRole(familyId, userId, FamilyRole.OWNER)) {
			throw new BusinessException(ErrorCode.NOT_FAMILY_OWNER);
		}
	}

	// 요청으로 받은 회원들이 모두 이 가족의 멤버인지 검사한다. 한 명이라도 아니면 FAMILY_MEMBER_NOT_FOUND.
	@Transactional(readOnly = true)
	public void validateMembers(Long familyId, Collection<Long> userIds) {
		Collection<Long> distinct = new HashSet<>(userIds);
		if (distinct.isEmpty()) {
			return;
		}
		if (familyMemberRepository.countByFamilyIdAndUserIdIn(familyId, distinct) != distinct.size()) {
			throw new BusinessException(ErrorCode.FAMILY_MEMBER_NOT_FOUND);
		}
	}

	@Transactional(readOnly = true)
	public void validateSameFamily(Long userId, Long otherUserId) {
		Long familyId = familyMemberRepository.findFirstByUserId(userId)
				.map(member -> member.getFamily().getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));
		validateMember(familyId, otherUserId);
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
		if (owner) {
			eventPublisher.publishEvent(new FamilyDeletedEvent(familyId));
			familyRepository.delete(member.getFamily());
		}
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

	// 정지된 가족도 센다.
	@Transactional(readOnly = true)
	public long countFamilies() {
		return familyRepository.count();
	}

}
