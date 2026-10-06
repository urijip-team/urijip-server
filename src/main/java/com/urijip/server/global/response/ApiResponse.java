package com.urijip.server.global.response;

import com.urijip.server.global.exception.ErrorCode;

public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

	public record ErrorBody(String code, String message) {
	}

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static ApiResponse<Void> error(ErrorCode errorCode) {
		return error(errorCode.name(), errorCode.getMessage());
	}

	public static ApiResponse<Void> error(String code, String message) {
		return new ApiResponse<>(false, null, new ErrorBody(code, message));
	}

}
