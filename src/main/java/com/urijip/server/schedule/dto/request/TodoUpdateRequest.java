package com.urijip.server.schedule.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 보낸 필드만 바꾼다. 담당자와 마감일은 {@code null}을 보내면 지워진다.
 * record로는 "필드를 안 보냄"과 "{@code null}을 보냄"을 구분할 수 없어서(Jackson이 둘 다 같은 값을 넣는다),
 * setter가 불렸는지로 보냈는지를 기록한다.
 */
@Getter
@EqualsAndHashCode
@ToString
public class TodoUpdateRequest {

	@Pattern(regexp = ".*\\S.*", message = "비워 둘 수 없습니다")
	@Size(max = 50, message = "50자 이하여야 합니다")
	private String title;

	private Long assigneeId;

	@JsonIgnore
	private boolean assigneeIdSent;

	private LocalDate dueDate;

	@JsonIgnore
	private boolean dueDateSent;

	private Boolean done;

	public void setTitle(String title) {
		this.title = title;
	}

	public void setAssigneeId(Long assigneeId) {
		this.assigneeId = assigneeId;
		this.assigneeIdSent = true;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
		this.dueDateSent = true;
	}

	public void setDone(Boolean done) {
		this.done = done;
	}

}
