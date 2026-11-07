package com.urijip.server.notice.entity;

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

// ERD에 created_at이 없어 BaseTimeEntity를 상속하지 않는다. 푸시 발송(FCM)은 아직 없어 isPushed는 늘 false다.
@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

	public static final int MAX_TITLE_LENGTH = 100;

	public static final int MAX_CONTENT_LENGTH = 5000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = MAX_TITLE_LENGTH)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "is_pushed", nullable = false)
	private boolean pushed;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	public Notice(User createdBy, String title, String content) {
		this.createdBy = createdBy;
		this.title = title;
		this.content = content;
	}

}
