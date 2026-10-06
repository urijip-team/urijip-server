package com.urijip.server.global.exception;

import java.util.List;

public record ErrorResponse(String code, String message, List<InvalidField> errors) {

	public record InvalidField(String field, String message) {
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return of(errorCode, List.of());
	}

	public static ErrorResponse of(ErrorCode errorCode, List<InvalidField> errors) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errors);
	}

}
