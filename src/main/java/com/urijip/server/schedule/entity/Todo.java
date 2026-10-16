package com.urijip.server.schedule.entity;

import java.time.LocalDate;

import com.urijip.server.family.entity.Family;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "todos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id", nullable = false)
	private Family family;

	@Column(nullable = false, length = 50)
	private String title;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private User assignee;

	private LocalDate dueDate;

	@Column(name = "is_done", nullable = false)
	private boolean done;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	@Builder
	private Todo(Family family, String title, User assignee, LocalDate dueDate, User createdBy) {
		this.family = family;
		this.title = title;
		this.assignee = assignee;
		this.dueDate = dueDate;
		this.createdBy = createdBy;
	}

	public void changeTitle(String title) {
		this.title = title;
	}

	public void changeAssignee(User assignee) {
		this.assignee = assignee;
	}

	public void changeDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public void changeDone(boolean done) {
		this.done = done;
	}

}
