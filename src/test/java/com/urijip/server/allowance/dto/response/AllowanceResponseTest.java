package com.urijip.server.allowance.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.urijip.server.allowance.entity.AllowanceRequest;
import com.urijip.server.allowance.entity.AllowanceStatus;
import com.urijip.server.family.entity.Family;
import com.urijip.server.member.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AllowanceResponseTest {

	@Test
	void fromCopiesRequestWithSeoulOffset() {
		User mom = user(1L, "김엄마");
		User kid = user(2L, "김첫째");
		Family family = Family.builder().name("우리집").inviteCode("A3K9P2").owner(mom).build();
		ReflectionTestUtils.setField(family, "id", 3L);
		AllowanceRequest request = new AllowanceRequest(family, kid, mom, 30000, "문제집 사야 해요");
		ReflectionTestUtils.setField(request, "id", 45L);
		ReflectionTestUtils.setField(request, "createdAt", LocalDateTime.of(2026, 11, 8, 10, 0));

		AllowanceResponse response = AllowanceResponse.from(request);

		assertThat(response).isEqualTo(new AllowanceResponse(45L, 3L, new AllowanceResponse.Person(2L, "김첫째"),
				new AllowanceResponse.Person(1L, "김엄마"), 30000, "문제집 사야 해요", AllowanceStatus.REQUESTED,
				OffsetDateTime.parse("2026-11-08T10:00:00+09:00"), null));
	}

	@Test
	void fromIncludesCompletedAt() {
		User mom = user(1L, "김엄마");
		Family family = Family.builder().name("우리집").inviteCode("A3K9P2").owner(mom).build();
		AllowanceRequest request = new AllowanceRequest(family, user(2L, "김첫째"), mom, 30000, null);
		ReflectionTestUtils.setField(request, "status", AllowanceStatus.SENT);
		ReflectionTestUtils.setField(request, "completedAt", LocalDateTime.of(2026, 11, 8, 12, 30));

		AllowanceResponse response = AllowanceResponse.from(request);

		assertThat(response.status()).isEqualTo(AllowanceStatus.SENT);
		assertThat(response.completedAt()).isEqualTo(OffsetDateTime.parse("2026-11-08T12:30:00+09:00"));
	}

	private User user(Long id, String name) {
		User user = User.builder().email(name + "@example.com").password("pw").name(name).build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

}
