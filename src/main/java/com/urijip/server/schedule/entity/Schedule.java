package com.urijip.server.schedule.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.urijip.server.family.entity.Family;
import com.urijip.server.member.entity.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id", nullable = false)
	private Family family;

	@Column(nullable = false, length = 50)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String memo;

	@Column(nullable = false)
	private LocalDateTime startAt;

	@Column(nullable = false)
	private LocalDateTime endAt;

	@Column(nullable = false)
	private boolean allDay;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	@ElementCollection
	@CollectionTable(name = "schedule_participants", joinColumns = @JoinColumn(name = "schedule_id"))
	@Column(name = "user_id", nullable = false)
	private Set<Long> participantIds = new HashSet<>();

	@Builder
	private Schedule(Family family, String title, String memo, LocalDateTime startAt, LocalDateTime endAt,
			boolean allDay, User createdBy, Collection<Long> participantIds) {
		this.family = family;
		this.title = title;
		this.memo = memo;
		this.startAt = startAt;
		this.endAt = endAt;
		this.allDay = allDay;
		this.createdBy = createdBy;
		if (participantIds != null) {
			this.participantIds.addAll(participantIds);
		}
	}

	public void changeTitle(String title) {
		this.title = title;
	}

	public void changeMemo(String memo) {
		this.memo = memo;
	}

	public void changePeriod(LocalDateTime startAt, LocalDateTime endAt, boolean allDay) {
		this.startAt = startAt;
		this.endAt = endAt;
		this.allDay = allDay;
	}

	public void changeParticipants(Collection<Long> participantIds) {
		this.participantIds.clear();
		this.participantIds.addAll(participantIds);
	}

}
