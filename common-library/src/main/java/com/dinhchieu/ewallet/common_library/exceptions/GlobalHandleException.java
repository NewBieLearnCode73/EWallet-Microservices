package com.dinhchieu.ewallet.common_library.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalHandleException {
  @ExceptionHandler(AppException.class)
  public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
    ErrorCode errorCode = ex.getErrorCode();
    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(errorCode.getStatusCode()).body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
    ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(errorCode.getStatusCode()).body(errorResponse);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex,
      HttpServletRequest request) {
    ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(errorCode.getStatusCode()).body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
      HttpServletRequest request) {

    String specificMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
    ErrorCode errorCode = ErrorCode.INVALID_INPUT;
    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .message(specificMessage != null ? specificMessage : errorCode.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(errorCode.getStatusCode()).body(errorResponse);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex,
      HttpServletRequest request) {
    ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(errorCode.getStatusCode()).body(errorResponse);
  }

}