package com.urijip.server.allowance.dto.request;

import java.util.Locale;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;

// 월별 내역의 type 쿼리 파라미터. 명세서대로 소문자(sent, received)로 받고 대소문자는 가리지 않는다.
public enum AllowanceHistoryType {

	SENT, RECEIVED;

	public static AllowanceHistoryType from(String value) {
		if (value != null) {
			for (AllowanceHistoryType type : values()) {
				if (type.name().equals(value.trim().toUpperCase(Locale.ROOT))) {
					return type;
				}
			}
		}
		throw new BusinessException(ErrorCode.INVALID_INPUT);
	}

}
