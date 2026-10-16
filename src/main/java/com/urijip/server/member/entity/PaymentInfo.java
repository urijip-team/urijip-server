package com.urijip.server.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_infos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentInfo {

	@Id
	private Long userId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false, length = 20)
	private String bankName;

	@Column(nullable = false)
	private String accountNumberEnc;

	@Column(nullable = false, length = 4)
	private String accountLast4;

	@Column(nullable = false, length = 30)
	private String accountHolder;

	private String transferLink;

	public PaymentInfo(User user, String bankName, String accountNumberEnc, String accountLast4,
			String accountHolder, String transferLink) {
		this.user = user;
		update(bankName, accountNumberEnc, accountLast4, accountHolder, transferLink);
	}

	public void update(String bankName, String accountNumberEnc, String accountLast4, String accountHolder,
			String transferLink) {
		this.bankName = bankName;
		this.accountNumberEnc = accountNumberEnc;
		this.accountLast4 = accountLast4;
		this.accountHolder = accountHolder;
		this.transferLink = transferLink;
	}

}
