package com.urijip.server.chat.entity;

import com.urijip.server.family.entity.Family;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

// 가족마다 채팅방이 하나라 메시지는 가족에 바로 딸린다. 시스템 메시지는 보낸 사람이 없다(sender_id null).
@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id", nullable = false)
	private Family family;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_id")
	private User sender;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MessageType type;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	private ChatMessage(Family family, User sender, MessageType type, String content) {
		this.family = family;
		this.sender = sender;
		this.type = type;
		this.content = content;
	}

	public static ChatMessage text(Family family, User sender, String content) {
		return new ChatMessage(family, sender, MessageType.TEXT, content);
	}

}
