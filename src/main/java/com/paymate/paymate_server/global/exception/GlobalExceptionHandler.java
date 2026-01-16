package com.paymate.paymate_server.global.exception;

import com.paymate.paymate_server.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 모든 에러를 잡아서 처리하는 녀석
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        // 에러 로그를 콘솔에 찍어줌 (디버깅용)
        e.printStackTrace();

        // "실패" 메시지와 함께 500 에러(서버 잘못)를 보냄
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 에러가 발생했습니다: " + e.getMessage()));
    }
}
