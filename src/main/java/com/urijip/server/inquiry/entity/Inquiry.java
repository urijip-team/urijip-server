package com.urijip.server.inquiry.entity;

import java.time.LocalDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// ERD에 created_at이 없어 BaseTimeEntity를 상속하지 않는다. 답변 전에는 answer, answeredBy, answeredAt이 모두 null이다.
@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {

	public static final int MAX_TITLE_LENGTH = 100;

	public static final int MAX_TEXT_LENGTH = 2000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = MAX_TITLE_LENGTH)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(columnDefinition = "TEXT")
	private String answer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "answered_by")
	private User answeredBy;

	private LocalDateTime answeredAt;

	public Inquiry(User user, String title, String content) {
		this.user = user;
		this.title = title;
		this.content = content;
	}

	public boolean isAnswered() {
		return answeredAt != null;
	}

	public void answer(User admin, String answer) {
		this.answeredBy = admin;
		this.answer = answer;
		this.answeredAt = LocalDateTime.now(SeoulTime.ZONE);
	}

}
