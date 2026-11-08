package com.urijip.server.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;

import java.util.List;
import java.util.Set;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.push.PushMessage;
import com.urijip.server.global.push.PushSender;
import com.urijip.server.global.push.PushType;
import com.urijip.server.member.dto.request.DeviceTokenRequest;
import com.urijip.server.member.entity.Platform;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
import com.urijip.server.member.repository.DeviceTokenRepository;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.member.service.DeviceTokenService;
import com.urijip.server.notification.dto.request.NotificationSettingUpdateRequest;
import com.urijip.server.notification.repository.NotificationSettingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

// 커밋 뒤 다른 스레드에서 보내는지 보려고 테스트 트랜잭션을 쓰지 않는다. 만든 행은 끝나고 지운다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PushDispatcherTest {

	private static final PushMessage MESSAGE = PushMessage.of(PushType.SOS, "SOS", "첫째님이 SOS를 보냈습니다.");

	@MockitoBean
	private PushSender pushSender;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private DeviceTokenService deviceTokenService;

	@Autowired
	private DeviceTokenRepository deviceTokenRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private NotificationSettingService notificationSettingService;

	@Autowired
	private NotificationSettingRepository notificationSettingRepository;

	private User mom;

	private User dad;

	@BeforeEach
	void setUp() {
		mom = saveUser("push-mom@example.com");
		dad = saveUser("push-dad@example.com");
		deviceTokenService.register(mom.getId(), new DeviceTokenRequest("mom-phone", Platform.ANDROID));
		deviceTokenService.register(dad.getId(), new DeviceTokenRequest("dad-phone", Platform.IOS));
	}

	@AfterEach
	void tearDown() {
		transactionTemplate.executeWithoutResult(status -> {
			deviceTokenRepository.deleteByUserId(mom.getId());
			deviceTokenRepository.deleteByUserId(dad.getId());
		});
		notificationSettingRepository.deleteAllById(List.of(mom.getId(), dad.getId()));
		userRepository.deleteAll(List.of(mom, dad));
	}

	@Test
	void sendsToReceiversDevicesAfterCommit() {
		given(pushSender.send(any(), any())).willReturn(Set.of());

		transactionTemplate.executeWithoutResult(status -> notificationService.send(List.of(mom.getId()), MESSAGE));

		then(pushSender).should(timeout(3000)).send(List.of("mom-phone"), MESSAGE);
	}

	@Test
	void sendsOutsideTransactionToo() {
		given(pushSender.send(any(), any())).willReturn(Set.of());

		notificationService.send(List.of(mom.getId(), dad.getId()), MESSAGE);

		then(pushSender).should(timeout(3000)).send(
				argThat(tokens -> Set.copyOf(tokens).equals(Set.of("mom-phone", "dad-phone"))),
				eq(MESSAGE));
	}

	@Test
	void skipsReceiversWhoTurnedTypeOff() {
		given(pushSender.send(any(), any())).willReturn(Set.of());
		notificationSettingService.update(mom.getId(), new NotificationSettingUpdateRequest(false, null, null, null));
		PushMessage chat = PushMessage.of(PushType.CHAT, "우리집", "김아빠: 저녁 먹자");

		notificationService.send(List.of(mom.getId(), dad.getId()), chat);

		then(pushSender).should(timeout(3000)).send(List.of("dad-phone"), chat);
	}

	@Test
	void sendsNothingWhenAllReceiversTurnedTypeOff() {
		notificationSettingService.update(mom.getId(), new NotificationSettingUpdateRequest(false, null, null, null));

		notificationService.send(List.of(mom.getId()), PushMessage.of(PushType.CHAT, "우리집", "김아빠: 저녁 먹자"));

		then(pushSender).should(after(500).never()).send(any(), any());
	}

	@Test
	void sendsNothingWhenTransactionRollsBack() {
		transactionTemplate.executeWithoutResult(status -> {
			notificationService.send(List.of(mom.getId()), MESSAGE);
			status.setRollbackOnly();
		});

		then(pushSender).should(after(500).never()).send(any(), any());
	}

	@Test
	void sendsNothingWhenReceiversHaveNoDevice() {
		User kid = saveUser("push-kid@example.com");
		try {
			notificationService.send(List.of(kid.getId()), MESSAGE);

			then(pushSender).should(after(500).never()).send(any(), any());
		} finally {
			userRepository.delete(kid);
		}
	}

	@Test
	void deletesTokensThatCanNeverSucceed() throws InterruptedException {
		given(pushSender.send(any(), any())).willReturn(Set.of("mom-phone"));

		notificationService.send(List.of(mom.getId()), MESSAGE);

		then(pushSender).should(timeout(3000)).send(any(), any());
		waitUntilDeleted("mom-phone");
		assertThat(deviceTokenRepository.findByFcmToken("mom-phone")).isEmpty();
		assertThat(deviceTokenRepository.findByFcmToken("dad-phone")).isPresent();
	}

	@Test
	void senderFailureDoesNotPropagate() {
		given(pushSender.send(any(), any())).willThrow(new IllegalStateException("FCM down"));

		notificationService.send(List.of(mom.getId()), MESSAGE);

		then(pushSender).should(timeout(3000)).send(any(), any());
		assertThat(deviceTokenRepository.findByFcmToken("mom-phone")).isPresent();
	}

	@Test
	void broadcastSendsToActiveUsersOnly() {
		given(pushSender.send(any(), any())).willReturn(Set.of());
		User suspended = saveUser("push-suspended@example.com");
		deviceTokenService.register(suspended.getId(), new DeviceTokenRequest("suspended-phone", Platform.ANDROID));
		suspended.changeStatus(UserStatus.SUSPENDED);
		userRepository.save(suspended);
		try {
			transactionTemplate.executeWithoutResult(status -> notificationService.sendToAll(MESSAGE));

			then(pushSender).should(timeout(3000)).send(
					argThat(tokens -> tokens.containsAll(List.of("mom-phone", "dad-phone"))
							&& !tokens.contains("suspended-phone")),
					eq(MESSAGE));
		} finally {
			transactionTemplate.executeWithoutResult(status -> deviceTokenRepository.deleteByUserId(suspended.getId()));
			userRepository.delete(suspended);
		}
	}

	@Test
	void broadcastSkipsUsersWhoTurnedNoticeOff() {
		given(pushSender.send(any(), any())).willReturn(Set.of());
		notificationSettingService.update(dad.getId(), new NotificationSettingUpdateRequest(null, null, null, false));

		notificationService.sendToAll(MESSAGE);

		then(pushSender).should(timeout(3000)).send(
				argThat(tokens -> tokens.contains("mom-phone") && !tokens.contains("dad-phone")), eq(MESSAGE));
	}

	@Test
	void broadcastSendsNothingWhenTransactionRollsBack() {
		transactionTemplate.executeWithoutResult(status -> {
			notificationService.sendToAll(MESSAGE);
			status.setRollbackOnly();
		});

		then(pushSender).should(after(500).never()).send(any(), any());
	}

	// 토큰 삭제는 발송 다음에 같은 스레드에서 일어나므로 잠깐 기다린다.
	private void waitUntilDeleted(String token) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 3000;
		while (deviceTokenRepository.findByFcmToken(token).isPresent() && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
	}

	private User saveUser(String email) {
		return userRepository.save(User.builder().email(email).password("encoded-password").name("회원").build());
	}

}
