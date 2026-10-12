package com.urijip.server.family.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;

import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.request.FamilyOwnerTransferRequest;
import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.entity.FamilyRole;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(FamilyController.class)
@Import(SecurityConfig.class)
class FamilyControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private FamilyService familyService;

	@Test
	void createReturnsCreatedWithFamily() {
		given(familyService.create(1L, new FamilyCreateRequest("김씨네")))
				.willReturn(new FamilyResponse(3L, "김씨네", "A3K9P2"));

		MvcTestResult result = create(1L, """
				{"name": "김씨네"}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.name").isEqualTo("김씨네");
		assertThat(result).bodyJson().extractingPath("$.data.inviteCode").isEqualTo("A3K9P2");
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	@Test
	void createPassesUserIdOfAccessToken() {
		create(7L, """
				{"name": "김씨네"}
				""");

		then(familyService).should().create(7L, new FamilyCreateRequest("김씨네"));
	}

	@Test
	void createWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/families")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "김씨네"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(familyService).should(never()).create(any(), any());
	}

	@Test
	void createByUserWhoAlreadyHasFamilyReturnsConflict() {
		given(familyService.create(any(), any()))
				.willThrow(new BusinessException(ErrorCode.ALREADY_FAMILY_MEMBER));

		MvcTestResult result = create(1L, """
				{"name": "김씨네"}
				""");

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("ALREADY_FAMILY_MEMBER");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"name\": \"\"}", "{\"name\": \" \"}"})
	void createWithoutNameReturnsBadRequest(String body) {
		MvcTestResult result = create(1L, body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains("name: ");
		then(familyService).should(never()).create(any(), any());
	}

	@Test
	void createWithNameLongerThanTwentyCharactersReturnsBadRequest() {
		MvcTestResult result = create(1L, "{\"name\": \"" + "가".repeat(21) + "\"}");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.message").isEqualTo("name: 20자 이하여야 합니다");
		then(familyService).should(never()).create(any(), any());
	}

	@Test
	void createWithTwentyCharacterNameIsAccepted() {
		MvcTestResult result = create(1L, "{\"name\": \"" + "가".repeat(20) + "\"}");

		assertThat(result).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void joinReturnsOkWithFamily() {
		given(familyService.join(2L, new FamilyJoinRequest("A3K9P2")))
				.willReturn(new FamilyResponse(3L, "김씨네", "A3K9P2"));

		MvcTestResult result = join(2L, """
				{"inviteCode": "A3K9P2"}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.name").isEqualTo("김씨네");
		assertThat(result).bodyJson().extractingPath("$.data.inviteCode").isEqualTo("A3K9P2");
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	@Test
	void joinPassesUserIdOfAccessToken() {
		join(7L, """
				{"inviteCode": "A3K9P2"}
				""");

		then(familyService).should().join(7L, new FamilyJoinRequest("A3K9P2"));
	}

	@Test
	void joinWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/families/join")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"inviteCode": "A3K9P2"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(familyService).should(never()).join(any(), any());
	}

	@Test
	void joinWithUnknownInviteCodeReturnsNotFound() {
		given(familyService.join(any(), any())).willThrow(new BusinessException(ErrorCode.INVITE_CODE_NOT_FOUND));

		MvcTestResult result = join(2L, """
				{"inviteCode": "A3K9P2"}
				""");

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVITE_CODE_NOT_FOUND");
	}

	@Test
	void joinByUserWhoAlreadyHasFamilyReturnsConflict() {
		given(familyService.join(any(), any())).willThrow(new BusinessException(ErrorCode.ALREADY_FAMILY_MEMBER));

		MvcTestResult result = join(2L, """
				{"inviteCode": "A3K9P2"}
				""");

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("ALREADY_FAMILY_MEMBER");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"inviteCode\": \"\"}", "{\"inviteCode\": \" \"}"})
	void joinWithoutInviteCodeReturnsBadRequest(String body) {
		MvcTestResult result = join(2L, body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains("inviteCode: ");
		then(familyService).should(never()).join(any(), any());
	}

	@Test
	void getMyFamilyReturnsFamilyOfAccessTokenUser() {
		given(familyService.getMyFamily(2L)).willReturn(new FamilyResponse(3L, "김씨네", "A3K9P2"));

		MvcTestResult result = getMyFamily(2L);

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.name").isEqualTo("김씨네");
		assertThat(result).bodyJson().extractingPath("$.data.inviteCode").isEqualTo("A3K9P2");
	}

	@Test
	void getMyFamilyByUserWithoutFamilyReturnsNotFound() {
		given(familyService.getMyFamily(any())).willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		MvcTestResult result = getMyFamily(2L);

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_FOUND");
	}

	@Test
	void getMyFamilyWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.get().uri("/api/families/me").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(familyService).should(never()).getMyFamily(any());
	}

	@Test
	void getMembersReturnsMemberList() {
		given(familyService.getMembers(3L, 2L)).willReturn(List.of(
				new FamilyMemberResponse(1L, "김엄마", "https://example.com/mom.png", "엄마", FamilyRole.OWNER),
				new FamilyMemberResponse(2L, "김아빠", null, null, FamilyRole.MEMBER)));

		MvcTestResult result = getMembers(2L, "3");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.length()").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.data[0].userId").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data[0].name").isEqualTo("김엄마");
		assertThat(result).bodyJson().extractingPath("$.data[0].profileImageUrl").isEqualTo("https://example.com/mom.png");
		assertThat(result).bodyJson().extractingPath("$.data[0].nickname").isEqualTo("엄마");
		assertThat(result).bodyJson().extractingPath("$.data[0].role").isEqualTo("OWNER");
		assertThat(result).bodyJson().extractingPath("$.data[1].role").isEqualTo("MEMBER");
	}

	@Test
	void getMembersReturnsNullForMissingNicknameAndProfileImage() {
		given(familyService.getMembers(3L, 2L)).willReturn(List.of(
				new FamilyMemberResponse(2L, "김아빠", null, null, FamilyRole.MEMBER)));

		MvcTestResult result = getMembers(2L, "3");

		assertThat(result).bodyJson().extractingPath("$.data[0].nickname").isNull();
		assertThat(result).bodyJson().extractingPath("$.data[0].profileImageUrl").isNull();
	}

	@Test
	void getMembersByUserWhoIsNotMemberReturnsForbidden() {
		given(familyService.getMembers(any(), any())).willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = getMembers(2L, "3");

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void getMembersWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.get().uri("/api/families/3/members").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(familyService).should(never()).getMembers(any(), any());
	}

	@Test
	void getMembersWithNonNumericFamilyIdReturnsBadRequest() {
		MvcTestResult result = getMembers(2L, "abc");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		then(familyService).should(never()).getMembers(any(), any());
	}

	@Test
	void reissueInviteCodeReturnsFamilyWithNewCode() {
		given(familyService.reissueInviteCode(3L, 1L)).willReturn(new FamilyResponse(3L, "김씨네", "B7B7B7"));

		MvcTestResult result = reissueInviteCode(1L, 3L);

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.inviteCode").isEqualTo("B7B7B7");
	}

	@Test
	void reissueInviteCodeByOrdinaryMemberReturnsForbidden() {
		given(familyService.reissueInviteCode(any(), any())).willThrow(new BusinessException(ErrorCode.NOT_FAMILY_OWNER));

		MvcTestResult result = reissueInviteCode(2L, 3L);

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("NOT_FAMILY_OWNER");
	}

	@Test
	void reissueInviteCodeByUserWhoIsNotMemberReturnsForbidden() {
		given(familyService.reissueInviteCode(any(), any())).willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = reissueInviteCode(2L, 3L);

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void reissueInviteCodeWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/families/3/invite-code").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(familyService).should(never()).reissueInviteCode(any(), any());
	}

	@Test
	void transferOwnerReturnsMembersWithNewRoles() {
		given(familyService.transferOwner(3L, 1L, new FamilyOwnerTransferRequest(2L))).willReturn(List.of(
				new FamilyMemberResponse(1L, "김엄마", null, null, FamilyRole.MEMBER),
				new FamilyMemberResponse(2L, "김아빠", null, null, FamilyRole.OWNER)));

		MvcTestResult result = transferOwner(1L, 3L, """
				{"userId": 2}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data[0].role").isEqualTo("MEMBER");
		assertThat(result).bodyJson().extractingPath("$.data[1].userId").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.data[1].role").isEqualTo("OWNER");
	}

	@Test
	void transferOwnerByOrdinaryMemberReturnsForbidden() {
		given(familyService.transferOwner(any(), any(), any()))
				.willThrow(new BusinessException(ErrorCode.NOT_FAMILY_OWNER));

		MvcTestResult result = transferOwner(1L, 3L, """
				{"userId": 2}
				""");

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("NOT_FAMILY_OWNER");
	}

	@Test
	void transferOwnerToUserWhoIsNotMemberReturnsNotFound() {
		given(familyService.transferOwner(any(), any(), any()))
				.willThrow(new BusinessException(ErrorCode.FAMILY_MEMBER_NOT_FOUND));

		MvcTestResult result = transferOwner(1L, 3L, """
				{"userId": 2}
				""");

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_MEMBER_NOT_FOUND");
	}

	@Test
	void transferOwnerWithoutUserIdReturnsBadRequest() {
		MvcTestResult result = transferOwner(1L, 3L, "{}");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains("userId: ");
		then(familyService).should(never()).transferOwner(any(), any(), any());
	}

	@Test
	void transferOwnerWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.patch().uri("/api/families/3/owner")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"userId": 2}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(familyService).should(never()).transferOwner(any(), any(), any());
	}

	@Test
	void removeMemberReturnsOkWithoutData() {
		MvcTestResult result = removeMember(1L, 3L, 2L);

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data").isNull();
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	private MvcTestResult create(Long userId, String body) {
		return mvc.post().uri("/api/families")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private MvcTestResult join(Long userId, String body) {
		return mvc.post().uri("/api/families/join")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private MvcTestResult getMyFamily(Long userId) {
		return mvc.get().uri("/api/families/me")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.exchange();
	}

	private MvcTestResult getMembers(Long userId, String familyId) {
		return mvc.get().uri("/api/families/{familyId}/members", familyId)
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.exchange();
	}

	private MvcTestResult reissueInviteCode(Long userId, Long familyId) {
		return mvc.post().uri("/api/families/{familyId}/invite-code", familyId)
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.exchange();
	}

	private MvcTestResult transferOwner(Long userId, Long familyId, String body) {
		return mvc.patch().uri("/api/families/{familyId}/owner", familyId)
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private MvcTestResult removeMember(Long userId, Long familyId, Long targetUserId) {
		return mvc.delete().uri("/api/families/{familyId}/members/{userId}", familyId, targetUserId)
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.exchange();
	}

}
