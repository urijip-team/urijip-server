package com.urijip.server.family.entity;

import java.time.LocalDateTime;

import com.urijip.server.member.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "family_members",
		uniqueConstraints = @UniqueConstraint(columnNames = {"family_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FamilyMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id", nullable = false)
	private Family family;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FamilyRole role;

	private String nickname;

	// 채팅에서 마지막으로 읽은 메시지 id. 아직 읽은 적 없으면 null.
	private Long lastReadMessageId;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	@Builder
	private FamilyMember(Family family, User user, FamilyRole role) {
		this.family = family;
		this.user = user;
		this.role = role;
	}

	public void changeRole(FamilyRole role) {
		this.role = role;
	}

	// 앞으로만 움직인다. 이미 더 뒤까지 읽었으면 바꾸지 않고 false.
	public boolean markRead(Long messageId) {
		if (lastReadMessageId != null && lastReadMessageId >= messageId) {
			return false;
		}
		this.lastReadMessageId = messageId;
		return true;
	}

	public void changeNickname(String nickname) {
		this.nickname = (nickname == null || nickname.isBlank()) ? null : nickname;
	}

}
