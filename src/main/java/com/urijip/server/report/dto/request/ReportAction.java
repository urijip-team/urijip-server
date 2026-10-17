package com.urijip.server.report.dto.request;

// 관리자가 신고를 어떻게 처리할지. SUSPEND_USER는 회원 신고에만 쓰고, REJECT는 신고를 반려한다.
public enum ReportAction {

	SUSPEND_USER, REJECT

}
