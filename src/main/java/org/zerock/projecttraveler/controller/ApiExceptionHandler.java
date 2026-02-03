package org.zerock.projecttraveler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.zerock.projecttraveler.dto.ApiResponse;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        // 메시지 통일하고 싶으면 e.getMessage() 대신 고정 문구로 바꿔도 됨
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }
}
