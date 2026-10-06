package com.urijip.server.global.exception;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
		ErrorCode errorCode = ex.getErrorCode();
		log.warn("Business exception: {}", errorCode);
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("Unexpected exception", ex);
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorResponse.InvalidField> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.InvalidField(error.getField(), error.getDefaultMessage()))
				.toList();
		return handleExceptionInternal(ex, ErrorResponse.of(ErrorCode.INVALID_INPUT, errors), headers, status,
				request);
	}

	// Spring MVC가 직접 던지는 예외(405, 415 등)도 상태 코드는 유지한 채 같은 응답 형식으로 내보낸다.
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		if (!(body instanceof ErrorResponse)) {
			HttpStatus status = HttpStatus.valueOf(statusCode.value());
			body = new ErrorResponse(status.name(), status.getReasonPhrase(), List.of());
		}
		return super.handleExceptionInternal(ex, body, headers, statusCode, request);
	}

}
