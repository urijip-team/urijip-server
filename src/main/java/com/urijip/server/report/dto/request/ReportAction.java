package com.urijip.server.report.dto.request;

// 관리자가 신고를 어떻게 처리할지. DELETE_PHOTO는 사진 신고에, SUSPEND_USER는 회원 신고에만 쓴다.
// REJECT는 신고를 반려한다.
public enum ReportAction {

	DELETE_PHOTO, SUSPEND_USER, REJECT

}
