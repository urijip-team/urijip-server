package com.urijip.server.member.service;

import com.urijip.server.family.service.FamilyService;
import com.urijip.server.member.dto.response.ProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

	private final UserService userService;
	private final FamilyService familyService;

	@Transactional(readOnly = true)
	public ProfileResponse getProfile(Long userId, Long targetUserId) {
		if (!userId.equals(targetUserId)) {
			familyService.validateSameFamily(userId, targetUserId);
		}

		return ProfileResponse.from(userService.getUser(targetUserId));
	}

}
