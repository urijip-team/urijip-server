package com.urijip.server.allowance.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.member.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

class AllowanceRequestTest {

	private User mom;

	private User kid;

	@BeforeEach
	void setUp() {
		mom = user(1L, "김엄마");
		kid = user(2L, "김첫째");
	}

	@Test
	void newRequestIsNotHandled() {
		AllowanceRequest request = new AllowanceRequest(null, kid, mom, 30000, "문제집");

		assertThat(request.isHandled()).isFalse();
		assertThat(request.getCompletedAt()).isNull();
	}

	@ParameterizedTest
	@EnumSource(value = AllowanceStatus.class, names = {"SENT", "REJECTED"})
	void completeChangesStatusAndRecordsTime(AllowanceStatus result) {
		AllowanceRequest request = new AllowanceRequest(null, kid, mom, 30000, "문제집");

		request.complete(result);

		assertThat(request.getStatus()).isEqualTo(result);
		assertThat(request.isHandled()).isTrue();
		assertThat(request.getCompletedAt()).isNotNull();
	}

	@Test
	void completeWithRequestedFails() {
		AllowanceRequest request = new AllowanceRequest(null, kid, mom, 30000, "문제집");

		assertThatThrownBy(() -> request.complete(AllowanceStatus.REQUESTED))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(request.isHandled()).isFalse();
	}

	@Test
	void participantsAreRequesterAndReceiver() {
		AllowanceRequest request = new AllowanceRequest(null, kid, mom, 30000, "문제집");

		assertThat(request.isParticipant(kid.getId())).isTrue();
		assertThat(request.isParticipant(mom.getId())).isTrue();
		assertThat(request.isParticipant(99L)).isFalse();
		assertThat(request.isReceiver(mom.getId())).isTrue();
		assertThat(request.isReceiver(kid.getId())).isFalse();
	}

	private User user(Long id, String name) {
		User user = User.builder().email(name + "@example.com").password("pw").name(name).build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

}
