package com.urijip.server.member.service;

import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.AesEncryptor;
import com.urijip.server.member.dto.request.PaymentInfoRequest;
import com.urijip.server.member.dto.response.AccountNumberResponse;
import com.urijip.server.member.dto.response.PaymentInfoResponse;
import com.urijip.server.member.entity.PaymentInfo;
import com.urijip.server.member.repository.PaymentInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentInfoService {

	private final PaymentInfoRepository paymentInfoRepository;
	private final UserService userService;
	private final FamilyService familyService;
	private final AesEncryptor aesEncryptor;

	@Transactional
	public PaymentInfoResponse register(Long userId, PaymentInfoRequest request) {
		String accountNumber = request.accountNumber();
		String encrypted = aesEncryptor.encrypt(accountNumber);
		String last4 = accountNumber.substring(accountNumber.length() - 4);

		PaymentInfo paymentInfo = paymentInfoRepository.findById(userId).orElse(null);
		if (paymentInfo == null) {
			paymentInfo = paymentInfoRepository.save(new PaymentInfo(userService.getUser(userId), request.bankName(),
					encrypted, last4, request.accountHolder(), request.transferLink()));
		} else {
			paymentInfo.update(request.bankName(), encrypted, last4, request.accountHolder(), request.transferLink());
		}

		return PaymentInfoResponse.from(paymentInfo);
	}

	@Transactional(readOnly = true)
	public PaymentInfoResponse getPaymentInfo(Long userId, Long targetUserId) {
		return PaymentInfoResponse.from(getAccessiblePaymentInfo(userId, targetUserId));
	}

	@Transactional(readOnly = true)
	public AccountNumberResponse getAccountNumber(Long userId, Long targetUserId) {
		PaymentInfo paymentInfo = getAccessiblePaymentInfo(userId, targetUserId);
		return new AccountNumberResponse(aesEncryptor.decrypt(paymentInfo.getAccountNumberEnc()));
	}

	private PaymentInfo getAccessiblePaymentInfo(Long userId, Long targetUserId) {
		if (!userId.equals(targetUserId)) {
			familyService.validateSameFamily(userId, targetUserId);
		}

		return paymentInfoRepository.findById(targetUserId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_INFO_NOT_FOUND));
	}

}
