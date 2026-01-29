package com.paymate.paymate_server.domain.store.dto;

import com.paymate.paymate_server.domain.store.enums.StorePayRule;
import com.paymate.paymate_server.domain.store.enums.TaxType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class StoreRequest {
    private Long userId; // 임시: 로그인 구현 전이라 사장님 ID를 직접 받겠습니다.

    private String businessNumber; // 사업자 번호
    private String ownerName;      // 대표자명
    private String storeName;      // 상호명
    private LocalDate openingDate; // 개업일
    private String address;        // 주소
    private String detailAddress;  // 상세주소

    // Enum은 String으로 들어오면 자동으로 변환됩니다 (JSON 파싱 시)
    private TaxType taxType;       // 과세 유형
    private String category;       // 업종
    private String storePhone;     // 매장 전화번호
    private String wifiInfo;       // 와이파이

    // 위치 정보 (출근 확인용)
    private Double latitude;      // 위도
    private Double longitude;     // 경도

    // 급여 정산 정보
    private Integer payDay;        // 급여일
    private StorePayRule payRule;  // 급여 규칙
    private String bankName;       // 은행명
    private String accountNumber;  // 계좌번호 (평문 -> DB 저장시 자동 암호화됨)


    private String verificationToken;
}