package com.urijip.server.notice.entity;

import com.urijip.server.global.entity.BaseTimeEntity;
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

// ERD에는 created_at이 없지만 앱 목록에 날짜를 보여 주려고 넣었다. isPushed는 작성할 때 전체 푸시를 보냈는지다.
@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

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

	public void markPushed() {
		this.pushed = true;
	}

	// null인 값은 바꾸지 않는다.
	public void update(String title, String content) {
		if (title != null) {
			this.title = title;
		}
		if (content != null) {
			this.content = content;
		}
	}

}
