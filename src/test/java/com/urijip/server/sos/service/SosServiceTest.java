package com.urijip.server.sos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.sos.dto.request.SosSendRequest;
import com.urijip.server.sos.dto.response.SosCreateResponse;
import com.urijip.server.sos.entity.SosAlert;
import com.urijip.server.sos.entity.SosStatus;
import com.urijip.server.sos.repository.SosAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SosServiceTest {

	private static final BigDecimal LATITUDE = new BigDecimal("35.1595");

	private static final BigDecimal LONGITUDE = new BigDecimal("126.8526");

	@Autowired
	private SosService sosService;

	@Autowired
	private SosAlertRepository sosAlertRepository;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User kid;

	private User stranger;

	private Long familyId;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		kid = saveUser("kid@example.com", "김첫째");
		stranger = saveUser("stranger@example.com", "남");
		FamilyResponse family = familyService.create(mom.getId(), new FamilyCreateRequest("우리집"));
		familyService.join(kid.getId(), new FamilyJoinRequest(family.inviteCode()));
		familyId = family.id();
	}

	@Test
	void sendSavesActiveAlert() {
		SosCreateResponse response = send(kid);

		assertThat(response.createdAt().getOffset().getId()).isEqualTo("+09:00");
		SosAlert saved = sosAlertRepository.findById(response.sosId()).orElseThrow();
		assertThat(saved.getFamily().getId()).isEqualTo(familyId);
		assertThat(saved.getUser().getId()).isEqualTo(kid.getId());
		assertThat(saved.getLatitude()).isEqualByComparingTo(LATITUDE);
		assertThat(saved.getLongitude()).isEqualByComparingTo(LONGITUDE);
		assertThat(saved.getStatus()).isEqualTo(SosStatus.ACTIVE);
	}

	@Test
	void sendRoundsLocationToSevenDecimals() {
		SosCreateResponse response = sosService.send(kid.getId(), new SosSendRequest(familyId,
				new BigDecimal("35.12345678"), new BigDecimal("126.12345674")));

		SosAlert saved = sosAlertRepository.findById(response.sosId()).orElseThrow();
		assertThat(saved.getLatitude()).isEqualByComparingTo("35.1234568");
		assertThat(saved.getLongitude()).isEqualByComparingTo("126.1234567");
	}

	@Test
	void sendAgainWhileActiveCreatesNewAlert() {
		SosCreateResponse first = send(kid);
		SosCreateResponse second = send(kid);

		assertThat(second.sosId()).isNotEqualTo(first.sosId());
	}

	@Test
	void sendByStrangerFails() {
		assertThatThrownBy(() -> send(stranger))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FAMILY_NOT_MEMBER));
		assertThat(sosAlertRepository.findAll()).isEmpty();
	}

	private SosCreateResponse send(User user) {
		return sosService.send(user.getId(), new SosSendRequest(familyId, LATITUDE, LONGITUDE));
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
