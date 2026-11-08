package com.urijip.server.adminlog.entity;

// 관리자가 한 변경 작업의 종류. 자세한 내용(바뀐 상태, 처리 방법 등)은 AdminLog.detail에 남긴다.
public enum AdminAction {

	CHANGE_USER_STATUS,
	CHANGE_FAMILY_STATUS,
	HANDLE_REPORT,
	ANSWER_INQUIRY,
	CREATE_NOTICE,
	UPDATE_NOTICE,
	DELETE_NOTICE

}
