package com.urijip.server.member.dto.response;

import com.urijip.server.member.entity.PaymentInfo;

public record PaymentInfoResponse(String bankName, String maskedAccount, String accountHolder,
		String transferLink) {

	public static PaymentInfoResponse from(PaymentInfo paymentInfo) {
		return new PaymentInfoResponse(paymentInfo.getBankName(), "****-" + paymentInfo.getAccountLast4(),
				paymentInfo.getAccountHolder(), paymentInfo.getTransferLink());
	}

}
