package com.urijip.server.family.entity;

import com.urijip.server.global.entity.BaseTimeEntity;
import com.urijip.server.member.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "families")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Family extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, unique = true, length = 6, columnDefinition = "char(6)")
	private String inviteCode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FamilyStatus status;

	@Builder
	private Family(String name, String inviteCode, User owner) {
		this.name = name;
		this.inviteCode = inviteCode;
		this.owner = owner;
		this.status = FamilyStatus.ACTIVE;
	}

	public void changeInviteCode(String inviteCode) {
		this.inviteCode = inviteCode;
	}

}
