package com.paymate.paymate_server.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private String status;  // "success" 또는 "fail"
    private String message; // 메시지 (예: "로그인 성공")
    private T data;         // 실제 데이터

    // 1. 성공했을 때 (데이터만)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", "요청이 성공했습니다.", data);
    }

    // 2. 성공했을 때 (메시지 + 데이터)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data);
    }

    // 3. 실패했을 때 (메시지만)
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("fail", message, null);
    }
}